package frample.com.frample_backend.dtos;

import frample.com.frample_backend.entities.SampleType;
import frample.com.frample_backend.entities.Scale;

public record SampleFilter(
    String q,
    String genre,
    SampleType sampleType,
    String musicalKey,
    Scale scale,
    Integer bpmMin,
    Integer bpmMax
) {
}