package frample.com.frample_backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import frample.com.frample_backend.entities.Role;
import frample.com.frample_backend.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class JwtService 
{
	private final SecretKey key;
	private final long accessTtlSeconds;
	private final long refreshTtlSeconds;
	private final String issuer;
	
	public JwtService(
			@Value("${security.jwt.secret}") String secret ,
			@Value("${security.jwt.access-ttl-seconds}") long accessTtlSeconds,
			@Value("${security.jwt.refresh-ttl-seconds}") long refreshTtlSeconds,
			@Value("${security.jwt.issuer}") String issuer
			)
	{
		if(secret==null || secret.length()<64)
		{
			throw new IllegalArgumentException("Invalid secret");
		}
		
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTtlSeconds = accessTtlSeconds;
		this.refreshTtlSeconds = refreshTtlSeconds;
		this.issuer = issuer;
		
	}
	
	//generate token:
	public String generateAccessToken(User user)
	{
		Instant now = Instant.now();
		List<String> roles = user.getRoles() == null ? List.of() : user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(user.getUserId().toString())
				.issuer(issuer)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
				.claims(Map.of(
						"username",user.getUsername(),
						"roles" , roles,
						"typ","access"
						))
				.signWith(key, SignatureAlgorithm.HS512).compact();
	}
	
	// Generate Refresh Token (Long-lived)
		public String generateRefreshToken(User user, String Jti)
		{
			Instant now = Instant.now();
			
			return Jwts.builder()
					.id(Jti)
					.subject(user.getUserId().toString())
					.issuer(issuer)
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
					.claim(
							"typ", "refresh"
							)
					.signWith(key, SignatureAlgorithm.HS512)
					.compact();
		}
		
		//Parse the token
		
		public Jws<Claims> parse(String token)
		{
			try {
				return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
				
			}
			catch(JwtException e)
			{
				throw e;
			}
		}
		
		public boolean isAccessToken(String token)
		{
			Claims c = parse(token).getPayload();
			return "access".equals(c.get("typ"));
		}
		public boolean isRefreshToken(String token)
		{
		    Claims c = parse(token).getPayload();
		    return "refresh".equals(c.get("typ"));
		}
		public Long getUserId(String token)
		{
			Claims c = parse(token).getPayload();
			return Long.valueOf(c.getSubject());
		}
		
		public String getJti(String token)
		{
			return parse(token).getPayload().getId();
		}
		
		
}
