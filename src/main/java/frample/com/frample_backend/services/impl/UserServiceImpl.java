package frample.com.frample_backend.services.impl;

import java.util.Set;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import frample.com.frample_backend.dtos.UserDto;
import frample.com.frample_backend.entities.Provider;
import frample.com.frample_backend.entities.Role;
import frample.com.frample_backend.entities.User;
import frample.com.frample_backend.exceptions.ResourceNotFoundException;
import frample.com.frample_backend.repositories.RoleRepository;
import frample.com.frample_backend.repositories.UserRepository;
import frample.com.frample_backend.services.UserService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final ModelMapper modelMapper;
	
	private static final String DEFAULT_ROLE = "ROLE_USER";


	private final RoleRepository roleRepository;

	@Override
	public UserDto createUser(UserDto userDto) {

		if (userDto.getUsername() == null || userDto.getUsername().isBlank())
		{
		    throw new IllegalArgumentException("Username is Required");
		}
		if (userDto.getUsername().length() < 3)
		{
		    throw new IllegalArgumentException("Username must be at least 3 characters");
		}
		if (userRepository.existsByUsername(userDto.getUsername()))
		{
		    throw new IllegalArgumentException("Username is already present");
		}
		if (userDto.getUsername().equalsIgnoreCase(userDto.getPassword()))
		{
		    throw new IllegalArgumentException("Password cannot be the same as username");
		}

		User user = modelMapper.map(userDto, User.class);
		user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);
		user.setStat(true);
		user.setRoles(Set.of(getOrCreateDefaultRole()));

		User savedUser = userRepository.save(user);
		return modelMapper.map(savedUser, UserDto.class);
	}

	@Override
	public User findOrCreateOAuthUser(String email, String displayName, Provider provider) {
		return userRepository.findByUserEmailId(email)
				.orElseGet(() -> {
					User newUser = User.builder()
							.username(generateUniqueUsername(displayName != null ? displayName : email))
							.userEmailId(email)
							.artistName(displayName)
							.password(null)
							.provider(provider)
							.stat(true)
							.roles(Set.of(getOrCreateDefaultRole()))
							.build();
					return userRepository.save(newUser);
				});
	}

	private Role getOrCreateDefaultRole() {
		return roleRepository.findByName(DEFAULT_ROLE)
				.orElseGet(() -> roleRepository.save(Role.builder().name(DEFAULT_ROLE).build()));
	}

	private String generateUniqueUsername(String base) {
		String candidate = base.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
		if (candidate.length() > 15) candidate = candidate.substring(0, 15);
		if (candidate.isBlank()) candidate = "user";

		String finalUsername = candidate;
		while (userRepository.existsByUsername(finalUsername)) {
			finalUsername = candidate + UUID.randomUUID().toString().substring(0, 4);
		}
		return finalUsername;
	}
	@Override
	public UserDto getUserByUsername(String username) {
		
		if(username == null || username.isBlank())
		{
			throw new IllegalArgumentException("Username is Empty!");
		}
		
		User user = userRepository.findByUsername(username)
		.orElseThrow(()-> new ResourceNotFoundException("User not Not found with Username: "+username));
		
		
		return modelMapper.map(user,UserDto.class);
	}

	@Override
	public UserDto getUserByEmail(String email) {
		// TODO Auto-generated method stub
		if(email == null || email.isBlank())
		{
			throw new IllegalArgumentException("Username is Empty!");
		}
		
		User user = userRepository.findByUserEmailId(email)
		.orElseThrow(()-> new ResourceNotFoundException("User not Not found with email: "+email));
		
		
		return modelMapper.map(user,UserDto.class);
	}

	@Override
	public UserDto updateUser(UserDto userDto, Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteUser(Long userId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Iterable<UserDto> getAllUsers() {
		// TODO Auto-generated method stub
		return userRepository.findAll()
				.stream()
				.map( user -> modelMapper.map(user, UserDto.class))
				.toList();
	}
	




}
