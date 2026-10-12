package frample.com.frample_backend.services;

import org.springframework.data.domain.Page;

import frample.com.frample_backend.dtos.MediaUrlResponse;
import frample.com.frample_backend.dtos.PresignedUploadRequest;
import frample.com.frample_backend.dtos.PresignedUploadResponse;
import frample.com.frample_backend.dtos.SampleDto;
import frample.com.frample_backend.dtos.SampleFilter;
import frample.com.frample_backend.dtos.SaveSampleRequest;
import frample.com.frample_backend.dtos.UpdateSampleRequest;
import frample.com.frample_backend.entities.User;

public interface SampleService {
    PresignedUploadResponse createPresignedUpload(PresignedUploadRequest request);
    SampleDto saveSample(SaveSampleRequest request, User uploader);
    Page<SampleDto> getSamples(SampleFilter filter, int page, int size);
    MediaUrlResponse getStreamUrl(Long sampleId);
    MediaUrlResponse getDownloadUrl(Long sampleId);
    Page<SampleDto> getMySamples(Long userId, int page, int size);
    SampleDto updateSample(Long sampleId, UpdateSampleRequest request, User currentUser);
    void deleteSample(Long sampleId, User currentUser);
}