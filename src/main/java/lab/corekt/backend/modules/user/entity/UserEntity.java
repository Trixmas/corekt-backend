package lab.corekt.backend.modules.user.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.achievement.entity.HasAchievementsEntity;
import lab.corekt.backend.modules.project.project.entity.ProjectEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

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
