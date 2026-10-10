package frample.com.frample_backend.services.impl;

import java.time.Duration;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import frample.com.frample_backend.dtos.PresignedUploadRequest;
import frample.com.frample_backend.dtos.PresignedUploadResponse;
import frample.com.frample_backend.dtos.SampleDto;
import frample.com.frample_backend.dtos.SaveSampleRequest;
import frample.com.frample_backend.entities.Sample;
import frample.com.frample_backend.entities.User;
import frample.com.frample_backend.repositories.SampleRepository;
import frample.com.frample_backend.services.SampleService;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import frample.com.frample_backend.dtos.MediaUrlResponse;
import frample.com.frample_backend.exceptions.ResourceNotFoundException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;


@Service
@RequiredArgsConstructor
public class SampleServiceImpl implements SampleService {

    private final SampleRepository sampleRepository;
    private final S3Presigner s3Presigner;
    private final ModelMapper modelMapper;

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
    public Page<SampleDto> getAllSamples(Pageable pageable) {
        return sampleRepository.findAllByOrderByCreatedDttmDesc(pageable).map(this::toDto);
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
}