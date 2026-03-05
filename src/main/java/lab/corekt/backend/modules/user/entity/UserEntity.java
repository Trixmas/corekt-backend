package lab.corekt.backend.modules.user.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.achievement.entity.HasAchievementsEntity;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class UserEntity {

    @Id
    @UuidGenerator
    @GeneratedValue
    @Column
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 50)
    private String username;

    @Column
    private String mistralToken;

    @OneToMany(mappedBy = "owner")
    private List<ProjectEntity> projects;

    @OneToMany(mappedBy = "user")
    private List<HasAchievementsEntity> achievements;

    public UserEntity(String email, String candidateUsername) {
        this.email = email;
        this.username = candidateUsername;
    }

    public UserEntity() {

    }
}
