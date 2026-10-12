package frample.com.frample_backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import frample.com.frample_backend.dtos.MediaUrlResponse;
import frample.com.frample_backend.dtos.PresignedUploadRequest;
import frample.com.frample_backend.dtos.PresignedUploadResponse;
import frample.com.frample_backend.dtos.SampleDto;
import frample.com.frample_backend.dtos.SampleFilter;
import frample.com.frample_backend.dtos.SaveSampleRequest;
import frample.com.frample_backend.dtos.UpdateSampleRequest;
import frample.com.frample_backend.entities.SampleType;
import frample.com.frample_backend.entities.Scale;
import frample.com.frample_backend.entities.User;
import frample.com.frample_backend.services.SampleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


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
    public ResponseEntity<Page<SampleDto>> getSamples(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) SampleType sampleType,
            @RequestParam(required = false) String musicalKey,
            @RequestParam(required = false) Scale scale,
            @RequestParam(required = false) Integer bpmMin,
            @RequestParam(required = false) Integer bpmMax) {

        SampleFilter filter = new SampleFilter(q, genre, sampleType, musicalKey, scale, bpmMin, bpmMax);
        return ResponseEntity.ok(sampleService.getSamples(filter, page, size));
    }
    @GetMapping("/{id}/stream")
    public ResponseEntity<MediaUrlResponse> getStreamUrl(@PathVariable Long id) {
        return ResponseEntity.ok(sampleService.getStreamUrl(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<MediaUrlResponse> getDownloadUrl(@PathVariable Long id) {
        return ResponseEntity.ok(sampleService.getDownloadUrl(id));
    }
    @GetMapping("/mine")
    public ResponseEntity<Page<SampleDto>> getMySamples(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(sampleService.getMySamples(currentUser.getUserId(), page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SampleDto> updateSample(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSampleRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(sampleService.updateSample(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSample(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        sampleService.deleteSample(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}