package frample.com.frample_backend.dtos;

import java.time.Instant;

import frample.com.frample_backend.entities.SampleType;
import frample.com.frample_backend.entities.Scale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SampleDto {
    private Long sampleId;
    private String title;
    private String description;
    private Integer bpm;
    private SampleType sampleType;
    private String musicalKey;
    private Scale scale;
    private String timeSignature;
    private String genre;
    private String originalFileName;
    private Long fileSizeBytes;
    private Integer durationSeconds;
    private Integer downloadCount;
    private Integer playCount;
    private String uploaderUsername;
    private Instant createdDttm;
    private Instant updatedDttm;
    private String waveformPeaks;
}