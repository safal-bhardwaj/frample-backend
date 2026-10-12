package frample.com.frample_backend.dtos;

import frample.com.frample_backend.entities.SampleType;
import frample.com.frample_backend.entities.Scale;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSampleRequest(
    @NotBlank @Size(max = 150) String title,
    @Size(max = 1000) String description,
    @Min(1) @Max(999) Integer bpm,
    SampleType sampleType,
    @Size(max = 10) String musicalKey,
    Scale scale,
    @Size(max = 10) String timeSignature,
    @Size(max = 50) String genre
) {
}