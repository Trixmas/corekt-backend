package lab.corekt.backend.modules.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class UserEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private String email;

    @Column(nullable = false, length = 50)
    private String username;

    @Column
    private String mistralToken;

    public UserEntity(String email, String candidateUsername) {
        this.email = email;
        this.username = candidateUsername;
    }

    public UserEntity() {
        
    }
}
