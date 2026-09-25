package frample.com.frample_backend.dtos;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto 
{
	// Properties
	
	
		private Long userId;
		private String username;
		private String bio;
		private String profilePicUrl;
		private String artistName;
		private Instant createdDttm;


}
