package frample.com.frample_backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import frample.com.frample_backend.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}