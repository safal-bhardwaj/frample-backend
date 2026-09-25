package frample.com.frample_backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table (name = "roles")
public class Role 
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long roleId;
	
	@Column (unique = true, nullable = false)
	private String name;

}
