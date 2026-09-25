package frample.com.frample_backend.services;



import frample.com.frample_backend.dtos.UserDto;
import frample.com.frample_backend.entities.Provider;
import frample.com.frample_backend.entities.User;


public interface AuthService 
{
	
	
	UserDto registerUser(UserDto userDto);
	User findOrCreateOAuthUser(String email, String displayName, Provider provider);

}
