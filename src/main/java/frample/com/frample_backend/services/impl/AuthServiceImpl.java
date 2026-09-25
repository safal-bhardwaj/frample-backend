package frample.com.frample_backend.services.impl;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import frample.com.frample_backend.dtos.UserDto;
import frample.com.frample_backend.entities.Provider;
import frample.com.frample_backend.entities.User;
import frample.com.frample_backend.repositories.UserRepository;
import frample.com.frample_backend.services.AuthService;
import frample.com.frample_backend.services.UserService;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService
{
	private final UserService userService;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	
	@Override
	public UserDto registerUser(UserDto userDto)
	{
		userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
		UserDto userDto1 = userService.createUser(userDto);
		return userDto1;
	}
	@Override
	public User findOrCreateOAuthUser(String email, String displayName, Provider provider) {
		return userService.findOrCreateOAuthUser(email, displayName, provider);
	}

	


}
