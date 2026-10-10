package frample.com.frample_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import frample.com.frample_backend.dtos.PresignedUploadRequest;
import frample.com.frample_backend.dtos.PresignedUploadResponse;
import frample.com.frample_backend.dtos.SampleDto;
import frample.com.frample_backend.dtos.SaveSampleRequest;
import frample.com.frample_backend.entities.User;
import frample.com.frample_backend.services.SampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import frample.com.frample_backend.dtos.MediaUrlResponse;


@RestController
@RequestMapping("/api/v1/samples")
@RequiredArgsConstructor
public class SampleController {

    private final SampleService sampleService;

    @PostMapping("/presign-upload")
    public ResponseEntity<PresignedUploadResponse> presignUpload(@RequestBody PresignedUploadRequest request) {
        return ResponseEntity.ok(sampleService.createPresignedUpload(request));
    }

    @PostMapping
    public ResponseEntity<SampleDto> saveSample(
            @RequestBody SaveSampleRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sampleService.saveSample(request, currentUser));
    }
    @GetMapping
    public ResponseEntity<Page<SampleDto>> getAllSamples(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(sampleService.getAllSamples(PageRequest.of(page, size)));
    }
    @GetMapping("/{id}/stream")
    public ResponseEntity<MediaUrlResponse> getStreamUrl(@PathVariable Long id) {
        return ResponseEntity.ok(sampleService.getStreamUrl(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<MediaUrlResponse> getDownloadUrl(@PathVariable Long id) {
        return ResponseEntity.ok(sampleService.getDownloadUrl(id));
    }
}