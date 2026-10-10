package frample.com.frample_backend.services;

import frample.com.frample_backend.dtos.MediaUrlResponse;
import frample.com.frample_backend.dtos.PresignedUploadRequest;
import frample.com.frample_backend.dtos.PresignedUploadResponse;
import frample.com.frample_backend.dtos.SampleDto;
import frample.com.frample_backend.dtos.SaveSampleRequest;
import frample.com.frample_backend.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

public interface SampleService {
    PresignedUploadResponse createPresignedUpload(PresignedUploadRequest request);
    SampleDto saveSample(SaveSampleRequest request, User uploader);
    Page<SampleDto> getAllSamples(Pageable pageable);
    MediaUrlResponse getStreamUrl(Long sampleId);
    MediaUrlResponse getDownloadUrl(Long sampleId);
}