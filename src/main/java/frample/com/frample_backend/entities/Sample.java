package frample.com.frample_backend.entities;



import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "samples")
public class Sample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sampleId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    private Integer bpm;

    @Enumerated(EnumType.STRING)
    private SampleType sampleType;

    // e.g. "C", "F#", "Bb" — kept as free text since key naming conventions vary
    @Column(length = 10)
    private String musicalKey;

    @Enumerated(EnumType.STRING)
    private Scale scale;

    // e.g. "4/4", "3/4", "6/8"
    @Column(length = 10)
    private String timeSignature;

    @Column(length = 50)
    private String genre;

    // Internal R2 object key — never exposed to the client directly (see SampleDto)
    @Column(nullable = false, unique = true)
    private String fileKey;

    @Column(nullable = false)
    private String originalFileName;

    private String mimeType;

    private Long fileSizeBytes;

    private Integer durationSeconds;

    private Integer downloadCount;

    private Integer playCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    private Instant createdDttm;

    private Instant updatedDttm;
 // JSON array of ~100 normalized amplitude values (0-100), computed in the browser at upload time
    @Column(length = 4000)
    private String waveformPeaks;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdDttm == null) createdDttm = now;
        updatedDttm = now;
        if (downloadCount == null) downloadCount = 0;
        if (playCount == null) playCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDttm = Instant.now();
    }
}
