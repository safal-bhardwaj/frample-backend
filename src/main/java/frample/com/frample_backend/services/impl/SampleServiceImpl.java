package frample.com.frample_backend.services.impl;

import java.time.Duration;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import frample.com.frample_backend.dtos.MediaUrlResponse;
import frample.com.frample_backend.dtos.PresignedUploadRequest;
import frample.com.frample_backend.dtos.PresignedUploadResponse;
import frample.com.frample_backend.dtos.SampleDto;
import frample.com.frample_backend.dtos.SampleFilter;
import frample.com.frample_backend.dtos.SaveSampleRequest;
import frample.com.frample_backend.dtos.UpdateSampleRequest;
import frample.com.frample_backend.entities.Sample;
import frample.com.frample_backend.entities.User;
import frample.com.frample_backend.exceptions.BadRequestException;
import frample.com.frample_backend.exceptions.ForbiddenOperationException;
import frample.com.frample_backend.exceptions.ResourceNotFoundException;
import frample.com.frample_backend.repositories.SampleRepository;
import frample.com.frample_backend.repositories.SampleSpecifications;
import frample.com.frample_backend.services.SampleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleServiceImpl implements SampleService {

    private final SampleRepository sampleRepository;
    private final S3Presigner s3Presigner;
    private final ModelMapper modelMapper;
    private final S3Client s3Client;
    @Value("${app.upload.max-bytes}")
    private long maxUploadBytes;

    @Value("${app.upload.max-samples-per-user}")
    private int maxSamplesPerUser;

    @Value("${security.r2.bucket}")
    private String bucket;

    @Override
    public PresignedUploadResponse createPresignedUpload(PresignedUploadRequest request, User uploader) {
        if (request.fileName() == null || request.fileName().isBlank()) {
            throw new BadRequestException("File name is required");
        }
        if (request.contentType() == null || !request.contentType().startsWith("audio/")) {
            throw new BadRequestException("Only audio files are allowed");
        }
        if (request.fileSizeBytes() == null || request.fileSizeBytes() <= 0 || request.fileSizeBytes() > maxUploadBytes) {
            throw new BadRequestException("File must be between 1 byte and " + (maxUploadBytes / 1024 / 1024) + " MB");
        }
        enforceUploadQuota(uploader);

        // The user id in the key means a file can only be registered by the person it was issued to
        String fileKey = "samples/" + uploader.getUserId() + "/" + UUID.randomUUID() + "-" + sanitize(request.fileName());

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .contentType(request.contentType())
                .contentLength(request.fileSizeBytes())   // signed into the URL: R2 rejects a different size
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putRequest)
                .build();

        return new PresignedUploadResponse(s3Presigner.presignPutObject(presignRequest).url().toString(), fileKey);
    }

    @Override
    public SampleDto saveSample(SaveSampleRequest request, User uploader) {
        if (!request.fileKey().startsWith("samples/" + uploader.getUserId() + "/")) {
            throw new BadRequestException("Invalid file key");
        }
        enforceUploadQuota(uploader);

        // Trust R2, not the browser: inspect the file that was actually uploaded
        HeadObjectResponse head;
        try {
            head = s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(request.fileKey()).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new BadRequestException("Uploaded file not found. Please upload again.");
            }
            throw e;
        }

        String contentType = head.contentType();
        long size = head.contentLength();
        if (contentType == null || !contentType.startsWith("audio/") || size <= 0 || size > maxUploadBytes) {
            deleteObjectQuietly(request.fileKey());
            throw new BadRequestException("Uploaded file is not valid audio within the size limit");
        }

        Sample sample = Sample.builder()
                .title(request.title().trim())
                .description(request.description())
                .bpm(request.bpm())
                .sampleType(request.sampleType())
                .musicalKey(request.musicalKey())
                .scale(request.scale())
                .timeSignature(request.timeSignature())
                .genre(request.genre())
                .fileKey(request.fileKey())
                .originalFileName(request.originalFileName())
                .mimeType(contentType)          // from R2, not from the client
                .fileSizeBytes(size)            // from R2, not from the client
                .durationSeconds(request.durationSeconds())
                .waveformPeaks(request.waveformPeaks())
                .uploader(uploader)
                .build();

        return toDto(sampleRepository.save(sample));
    }

    private void enforceUploadQuota(User uploader) {
        if (sampleRepository.countByUploader_UserId(uploader.getUserId()) >= maxSamplesPerUser) {
            throw new ForbiddenOperationException("Upload limit reached (" + maxSamplesPerUser + " samples)");
        }
    }

    private void deleteObjectQuietly(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            log.warn("Could not remove R2 object {}: {}", key, e.getMessage());
        }
    }
    private SampleDto toDto(Sample sample) {
        SampleDto dto = modelMapper.map(sample, SampleDto.class);
        dto.setUploaderUsername(sample.getUploader().getUsername());
        return dto;
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    @Override
    public Page<SampleDto> getSamples(SampleFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),   // cap page size so nobody can request 100,000 rows
                Sort.by(Sort.Direction.DESC, "createdDttm"));

        return sampleRepository.findAll(SampleSpecifications.withFilters(filter), pageable).map(this::toDto);
    }
    @Override
    public MediaUrlResponse getStreamUrl(Long sampleId) {
        Sample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sample not found"));

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(sample.getFileKey())
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);

        sample.setPlayCount(sample.getPlayCount() + 1);
        sampleRepository.save(sample);

        return new MediaUrlResponse(presigned.url().toString());
    }

    @Override
    public MediaUrlResponse getDownloadUrl(Long sampleId) {
        Sample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sample not found"));

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(sample.getFileKey())
                .responseContentDisposition("attachment; filename=\"" + buildDownloadFileName(sample) + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);

        sample.setDownloadCount(sample.getDownloadCount() + 1);
        sampleRepository.save(sample);

        return new MediaUrlResponse(presigned.url().toString());
    }
 // Produces e.g. "Vocal Loop_Minor_96.wav"  (title + scale + bpm, missing parts skipped)
    private String buildDownloadFileName(Sample sample) {
        StringBuilder name = new StringBuilder(sample.getTitle().trim());

        if (sample.getScale() != null) {
            String scale = sample.getScale().name();
            name.append("_").append(scale.charAt(0)).append(scale.substring(1).toLowerCase());
        }
        if (sample.getBpm() != null) {
            name.append("_").append(sample.getBpm());
        }

        String original = sample.getOriginalFileName();
        int dot = original.lastIndexOf('.');
        String extension = dot >= 0 ? original.substring(dot) : "";

        return sanitize(name.toString()) + sanitize(extension);
    }
    
    @Override
    public Page<SampleDto> getMySamples(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdDttm"));
        return sampleRepository.findByUploader_UserId(userId, pageable).map(this::toDto);
    }

    @Override
    public SampleDto updateSample(Long sampleId, UpdateSampleRequest request, User currentUser) {
        Sample sample = getOwnedSample(sampleId, currentUser);

        sample.setTitle(request.title().trim());
        sample.setDescription(blankToNull(request.description()));
        sample.setBpm(request.bpm());
        sample.setSampleType(request.sampleType());
        sample.setMusicalKey(blankToNull(request.musicalKey()));
        sample.setScale(request.scale());
        sample.setTimeSignature(blankToNull(request.timeSignature()));
        sample.setGenre(blankToNull(request.genre()));

        return toDto(sampleRepository.save(sample));
    }

    @Override
    public void deleteSample(Long sampleId, User currentUser) {
        Sample sample = getOwnedSample(sampleId, currentUser);
        String fileKey = sample.getFileKey();

        // Database first: an orphaned file is harmless, a row pointing at a missing file is not
        sampleRepository.deleteById(sampleId);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(fileKey).build());
        } catch (Exception e) {
            log.warn("Sample {} deleted but R2 object {} could not be removed: {}", sampleId, fileKey, e.getMessage());
        }
    }

    private Sample getOwnedSample(Long sampleId, User currentUser) {
        Sample sample = sampleRepository.findBySampleId(sampleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sample not found"));
        if (!sample.getUploader().getUserId().equals(currentUser.getUserId())) {
            throw new ForbiddenOperationException("You can only modify your own samples");
        }
        return sample;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

	


	
}