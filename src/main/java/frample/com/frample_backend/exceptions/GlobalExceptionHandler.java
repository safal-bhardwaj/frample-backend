package frample.com.frample_backend.exceptions;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
	@ExceptionHandler(ForbiddenOperationException.class)
	public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenOperationException ex) {
	    return ResponseEntity.status(HttpStatus.FORBIDDEN)
	            .body(Map.of("message", ex.getMessage(), "statusCode", "403"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
	    String message = ex.getBindingResult().getFieldErrors().stream()
	            .map(e -> e.getField() + ": " + e.getDefaultMessage())
	            .findFirst()
	            .orElse("Invalid request");
	    return ResponseEntity.badRequest().body(Map.of("message", message, "statusCode", "400"));
	}
}
