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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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

    @Value("${security.r2.bucket}")
    private String bucket;

    @Override
    public PresignedUploadResponse createPresignedUpload(PresignedUploadRequest request) {
        String fileKey = "samples/" + UUID.randomUUID() + "-" + sanitize(request.fileName());

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        return new PresignedUploadResponse(presigned.url().toString(), fileKey);
    }

    @Override
    public SampleDto saveSample(SaveSampleRequest request, User uploader) {
        Sample sample = Sample.builder()
                .title(request.title())
                .description(request.description())
                .bpm(request.bpm())
                .sampleType(request.sampleType())
                .musicalKey(request.musicalKey())
                .scale(request.scale())
                .timeSignature(request.timeSignature())
                .genre(request.genre())
                .fileKey(request.fileKey())
                .originalFileName(request.originalFileName())
                .mimeType(request.mimeType())
                .fileSizeBytes(request.fileSizeBytes())
                .durationSeconds(request.durationSeconds())
                .uploader(uploader)
                .waveformPeaks(request.waveformPeaks())
                .build();

        Sample saved = sampleRepository.save(sample);
        return toDto(saved);
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