package lab.corekt.backend.modules.achievement.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "has_achievements")
@EntityListeners(AuditingEntityListener.class)
@IdClass(UserAchievementEntity.UserAchievementId.class)
public class UserAchievementEntity {

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Id
    @ManyToOne
    @JoinColumn(name = "achievement_id", nullable = false)
    private AchievementEntity achievement;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant acquiredAt;

    public UserAchievementEntity() {
    }

    public UserAchievementEntity(UserEntity user, AchievementEntity achievement) {
        this.user = user;
        this.achievement = achievement;
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAchievementId implements Serializable {
        private UUID user;
        private String achievement;
    }
}
