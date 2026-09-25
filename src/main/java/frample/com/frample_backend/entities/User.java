package frample.com.frample_backend.entities;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
 
@Entity
@Table (name = "Users")
public class User  implements UserDetails
{
	
	// Properties
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;
	
	@Column(unique = true ,length=20)
	private String username;
	
	private String password;
	
	@Column(length=500)
	private String bio;
	
	private String profilePicUrl;
	
	private String artistName;
	
	@Column(unique = true ,length=100)
	private String userEmailId;
	
	private Boolean stat = true;
	
	private Instant createdDttm;
	private Instant updtDttm;
	
	@Enumerated(EnumType.STRING)
	private Provider provider = Provider.LOCAL ;
	
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
	    name = "user_roles",
	    joinColumns = @JoinColumn(name = "user_id"),
	    inverseJoinColumns = @JoinColumn(name = "role_id")
	)
	private Set<Role> roles = new HashSet<>();
	
	@PrePersist
	protected void onCreate()
	{
		Instant now = Instant.now();
		if(createdDttm == null) createdDttm = now;
		updtDttm = null;
	}
	
	@PreUpdate
	
	protected void onUpdate()
	{
		Instant now = Instant.now();
		
		updtDttm = now;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		
		return roles.stream()
		        .map(role -> new SimpleGrantedAuthority(role.getName()))
		        .collect(Collectors.toList());
		
	}
	@Override
	public String getUsername() {
	    // Return email or username, whichever you use for login
	    return this.username; 
	}

	@Override
	public String getPassword() {
	    return this.password;
	}

	@Override
	public boolean isAccountNonExpired() {
	    return true; // Set to false if account expiration logic exists
	}

	@Override
	public boolean isAccountNonLocked() {
	    return true; // Set to false if account locking logic exists
	}

	@Override
	public boolean isCredentialsNonExpired() {
	    return true; // Set to false if password expiration logic exists
	}

	@Override
	public boolean isEnabled() {
	    return Boolean.TRUE.equals(this.stat); // Uses your 'stat' field to enable/disable user
	}

}
