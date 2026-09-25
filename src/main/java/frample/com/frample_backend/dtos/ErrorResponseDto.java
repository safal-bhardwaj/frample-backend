package frample.com.frample_backend.dtos;

import org.springframework.http.HttpStatus;

public record ErrorResponseDto(String message,
		HttpStatus status) 
{
}
