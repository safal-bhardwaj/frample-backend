package frample.com.frample_backend.repositories;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import frample.com.frample_backend.dtos.SampleFilter;
import frample.com.frample_backend.entities.Sample;
import frample.com.frample_backend.entities.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public final class SampleSpecifications {

    private SampleSpecifications() {
    }

    public static Specification<Sample> withFilters(SampleFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(f.q())) {
                String pattern = "%" + escapeLike(f.q().trim().toLowerCase()) + "%";
                Join<Sample, User> uploader = root.join("uploader");
                predicates.add(cb.or(
                    cb.like(cb.lower(root.<String>get("title")), pattern, '\\'),
                    cb.like(cb.lower(uploader.<String>get("username")), pattern, '\\')
                ));
            }
            if (hasText(f.genre())) {
                String pattern = "%" + escapeLike(f.genre().trim().toLowerCase()) + "%";
                predicates.add(cb.like(cb.lower(root.<String>get("genre")), pattern, '\\'));
            }
            if (f.sampleType() != null) {
                predicates.add(cb.equal(root.get("sampleType"), f.sampleType()));
            }
            if (hasText(f.musicalKey())) {
                predicates.add(cb.equal(cb.lower(root.<String>get("musicalKey")), f.musicalKey().trim().toLowerCase()));
            }
            if (f.scale() != null) {
                predicates.add(cb.equal(root.get("scale"), f.scale()));
            }
            if (f.bpmMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("bpm"), f.bpmMin()));
            }
            if (f.bpmMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Integer>get("bpm"), f.bpmMax()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    // Stops a user typing "%" or "_" from acting as a wildcard
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}