package lab.corekt.backend.modules.user.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.achievement.entity.UserAchievementEntity;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
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
    @Column(name = "user_id")
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 50)
    private String username;

    @Column
    private String mistralToken;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ProjectEntity> projects;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<UserAchievementEntity> achievements;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    public UserEntity(String email, String candidateUsername) {
        this.email = email;
        this.username = candidateUsername;
    }
}
