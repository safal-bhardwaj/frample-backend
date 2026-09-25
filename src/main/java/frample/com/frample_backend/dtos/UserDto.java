package frample.com.frample_backend.dtos;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import frample.com.frample_backend.entities.Provider;
import frample.com.frample_backend.entities.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
public class UserDto 
{
	// Properties
	
		
	private Long userId;
	private String username;
	private String password;
	private String bio;
	private String profilePicUrl;
	private String artistName;
	private String userEmailId;
	private Boolean stat = true;
	private Instant createdDttm;
	private Instant updtDttm;
	private Provider provider = Provider.LOCAL ;
	private Set<Role> roles = new HashSet<>();
		

}
