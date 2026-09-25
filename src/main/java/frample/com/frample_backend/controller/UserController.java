package frample.com.frample_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import frample.com.frample_backend.dtos.UserDto;
import frample.com.frample_backend.services.UserService;
import lombok.AllArgsConstructor;



@RestController
@RequestMapping("/api/v1/user")
@AllArgsConstructor
public class UserController 
{
	private final UserService userService;
	
	//Create User Api
	@PostMapping("/signup")
	public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto)
	{
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDto));
	}
	
	//get User by Username
	
	@GetMapping("/username/{username}")
	public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username)
	{
		return ResponseEntity.ok(userService.getUserByUsername(username));
	}
	
	//get User by email
	
		@GetMapping("/email/{email}")
		public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email)
		{
			return ResponseEntity.ok(userService.getUserByEmail(email));
		}
	//get All User
	@GetMapping
	public ResponseEntity<Iterable<UserDto>> getAllUsers()
	{
		return ResponseEntity.ok(userService.getAllUsers());
	}
	
}
