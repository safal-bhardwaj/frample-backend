package frample.com.frample_backend.dtos;

public record TokenResponse(String accessToken, long expiresIn, String tokenType, LoginResponseDto user) {
    public static TokenResponse of(String accessToken, long expiresIn, String tokenType, LoginResponseDto user) {
        return new TokenResponse(accessToken, expiresIn, tokenType, user);
    }
}