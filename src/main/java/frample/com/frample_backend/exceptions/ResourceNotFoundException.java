package frample.com.frample_backend.exceptions;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message)
	{
		super(message);
	}
	
	public ResourceNotFoundException()
	{
		super("Resource not found !!");
	}
}
