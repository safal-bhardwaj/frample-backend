package frample.com.frample_backend.dtos;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
}