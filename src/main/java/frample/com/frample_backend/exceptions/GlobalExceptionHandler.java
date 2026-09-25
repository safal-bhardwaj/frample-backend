package frample.com.frample_backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import frample.com.frample_backend.dtos.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler 
{
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException (ResourceNotFoundException exception){
		
		ErrorResponseDto resp = new ErrorResponseDto(exception.getMessage() , HttpStatus.NOT_FOUND);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException (IllegalArgumentException exception){
		
		ErrorResponseDto resp = new ErrorResponseDto(exception.getMessage() , HttpStatus.BAD_REQUEST);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
	}
}
