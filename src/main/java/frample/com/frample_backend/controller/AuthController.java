package frample.com.frample_backend.controller;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import frample.com.frample_backend.dtos.LoginRequest;
import frample.com.frample_backend.dtos.LoginResponseDto;
import frample.com.frample_backend.dtos.OAuthLoginRequest;
import frample.com.frample_backend.dtos.TokenResponse;
import frample.com.frample_backend.dtos.UserDto;
import frample.com.frample_backend.entities.Provider;
import frample.com.frample_backend.entities.User;
import frample.com.frample_backend.repositories.UserRepository;
import frample.com.frample_backend.security.GoogleTokenVerifier;
import frample.com.frample_backend.security.JwtService;
import frample.com.frample_backend.services.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController
{
	private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

	private final AuthService authService;
	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final ModelMapper mapper;
	private final GoogleTokenVerifier googleTokenVerifier; 

	@Value("${security.cookie.secure:false}")
	private boolean cookieSecure;

	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response)
	{
		Authentication authenticate = authenticate(loginRequest);
		User user = userRepository.findByUsername(loginRequest.username())
				.orElseThrow(() -> new BadCredentialsException("Invalid Username or Password"));

		if (!user.isEnabled())
		{
			throw new DisabledException("User is disabled");
		}

		String accessToken = jwtService.generateAccessToken(user);
		String refreshToken = jwtService.generateRefreshToken(user, UUID.randomUUID().toString());

		addRefreshTokenCookie(response, refreshToken);

		TokenResponse tokenResponse = TokenResponse.of(
				accessToken, jwtService.getAccessTtlSeconds(), "Bearer",
				mapper.map(user, LoginResponseDto.class));
		return ResponseEntity.ok(tokenResponse);
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(
			@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
			HttpServletResponse response)
	{
		if (refreshToken == null || !jwtService.isRefreshToken(refreshToken))
		{
			throw new BadCredentialsException("Invalid refresh token");
		}

		Long userId = jwtService.getUserId(refreshToken);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

		if (!user.isEnabled())
		{
			throw new DisabledException("User is disabled");
		}

		String newAccessToken = jwtService.generateAccessToken(user);
		String newRefreshToken = jwtService.generateRefreshToken(user, UUID.randomUUID().toString());

		addRefreshTokenCookie(response, newRefreshToken);

		TokenResponse tokenResponse = TokenResponse.of(
				newAccessToken, jwtService.getAccessTtlSeconds(), "Bearer",
				mapper.map(user, LoginResponseDto.class));
		return ResponseEntity.ok(tokenResponse);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletResponse response)
	{
		ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
				.httpOnly(true)
				.secure(cookieSecure)
				.sameSite("Lax")
				.path("/api/v1/auth")
				.maxAge(0)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/signup")
	public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto)
	{
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));
	}

	private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken)
	{
		ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
				.httpOnly(true)
				.secure(cookieSecure)
				.sameSite("Lax")
				.path("/api/v1/auth")
				.maxAge(jwtService.getRefreshTtlSeconds())
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private Authentication authenticate(LoginRequest loginRequest)
	{
		try {
			return authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));
		}
		catch (Exception e)
		{
			log.error("Login failed for username '{}': {} - {}",
					loginRequest.username(), e.getClass().getSimpleName(), e.getMessage());
			throw new BadCredentialsException("Invalid Username and Password");
		}
	}
	@PostMapping("/oauth/google")
	public ResponseEntity<TokenResponse> googleLogin(@RequestBody OAuthLoginRequest request, HttpServletResponse response)
	{
	   
		GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.idToken());

	    String email = payload.getEmail();
	    String name = (String) payload.get("name");

	    User user = authService.findOrCreateOAuthUser(email, name, Provider.GOOGLE);

	    if (!user.isEnabled())
	    {
	        throw new DisabledException("User is disabled");
	    }

	    String accessToken = jwtService.generateAccessToken(user);
	    String refreshToken = jwtService.generateRefreshToken(user, UUID.randomUUID().toString());

	    addRefreshTokenCookie(response, refreshToken);

	    TokenResponse tokenResponse = TokenResponse.of(
	            accessToken, jwtService.getAccessTtlSeconds(), "Bearer",
	            mapper.map(user, LoginResponseDto.class));
	    return ResponseEntity.ok(tokenResponse);
	}
}