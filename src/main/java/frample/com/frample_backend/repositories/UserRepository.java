package frample.com.frample_backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import frample.com.frample_backend.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User,Long>{
	
	Optional<User> findByUsername(String username);
	Optional<User> findByUserEmailId(String user_email_id);
	
	boolean existsByUsername(String username);
	boolean existsByUserEmailId(String user_email_id);
	

}
