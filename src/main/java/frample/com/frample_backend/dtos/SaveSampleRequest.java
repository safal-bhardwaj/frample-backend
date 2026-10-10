package frample.com.frample_backend.dtos;

import frample.com.frample_backend.entities.Scale;
import frample.com.frample_backend.entities.SampleType;

public record SaveSampleRequest(
	    String title,
	    String description,
	    Integer bpm,
	    SampleType sampleType,
	    String musicalKey,
	    Scale scale,
	    String timeSignature,
	    String genre,
	    String fileKey,
	    String originalFileName,
	    String mimeType,
	    Long fileSizeBytes,
	    Integer durationSeconds,
	    String waveformPeaks
	) {
	}