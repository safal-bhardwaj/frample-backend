package frample.com.frample_backend.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import frample.com.frample_backend.entities.Sample;

public interface SampleRepository extends JpaRepository<Sample, Long>, JpaSpecificationExecutor<Sample> {

    @Override
    @EntityGraph(attributePaths = "uploader")
    Page<Sample> findAll(Specification<Sample> spec, Pageable pageable);
    @EntityGraph(attributePaths = "uploader")
    Optional<Sample> findBySampleId(Long sampleId);

    @EntityGraph(attributePaths = "uploader")
    Page<Sample> findByUploader_UserId(Long userId, Pageable pageable);
}