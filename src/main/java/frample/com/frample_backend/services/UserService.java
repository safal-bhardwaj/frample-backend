package frample.com.frample_backend.services;

import frample.com.frample_backend.dtos.UserDto;
import frample.com.frample_backend.entities.Provider;
import frample.com.frample_backend.entities.User;


public interface UserService 
{
	//Create User
	UserDto createUser(UserDto userDto);
	
	//Get user by Username
	UserDto getUserByUsername(String username);
	
	//Get user by Email
	UserDto getUserByEmail(String email);
	
	//Update User
	UserDto updateUser(UserDto userDto, Long userId);
	
	//Delete User
	void deleteUser(Long userId);
	
	//get all user
	Iterable<UserDto> getAllUsers();
	
	User findOrCreateOAuthUser(String email, String displayName, Provider provider);

}
