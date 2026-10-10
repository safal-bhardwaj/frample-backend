package frample.com.frample_backend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import frample.com.frample_backend.entities.Sample;

public interface SampleRepository extends JpaRepository<Sample, Long> {

    @Query("SELECT s FROM Sample s JOIN FETCH s.uploader ORDER BY s.createdDttm DESC")
    Page<Sample> findAllByOrderByCreatedDttmDesc(Pageable pageable);
}