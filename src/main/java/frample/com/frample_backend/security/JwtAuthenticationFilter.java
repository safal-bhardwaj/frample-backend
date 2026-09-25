package frample.com.frample_backend.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import frample.com.frample_backend.entities.User;
import frample.com.frample_backend.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtService.isAccessToken(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    Jws<Claims> parse = jwtService.parse(token);
                    Claims payload = parse.getPayload();
                    Long userId = Long.valueOf(payload.getSubject());

                    Optional<User> userOptional = userRepository.findById(userId);

                    if (userOptional.isPresent()) {
                        User user = userOptional.get();

                        // Only authenticate if user account is enabled
                        if (user.isEnabled()) {
                            List<GrantedAuthority> authorities = user.getRoles() == null 
                                    ? List.of() 
                                    : user.getRoles().stream()
                                            .map(role -> new SimpleGrantedAuthority(role.getName()))
                                            .collect(Collectors.toList());

                            // Pass the full User object as the principal
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    user, 
                                    null, 
                                    authorities
                            );

                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                } catch (JwtException e) {
                    log.error("JWT validation failed: {}", e.getMessage());
                } catch (Exception e) {
                    log.error("Cannot set user authentication: {}", e.getMessage());
                }
            }
        }

        // Always called exactly ONCE at the end of the filter execution
        filterChain.doFilter(request, response);
    }
}