package frample.com.frample_backend.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import frample.com.frample_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// Must throw UsernameNotFoundException here - Spring Security's
		// DaoAuthenticationProvider specifically catches this type and
		// converts it into a clean BadCredentialsException.
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
	}
}