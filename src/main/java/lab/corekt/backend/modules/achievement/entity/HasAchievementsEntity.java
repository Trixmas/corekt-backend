package lab.corekt.backend.modules.achievement.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "has_achievements")
@EntityListeners(AuditingEntityListener.class)
@IdClass(HasAchievementsEntity.HasAchievementsId.class)
public class HasAchievementsEntity {

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Id
    @ManyToOne
    @JoinColumn(name = "achievement_name", nullable = false)
    private AchievementEntity achievement;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public HasAchievementsEntity() {
    }

    public HasAchievementsEntity(UserEntity user, AchievementEntity achievement) {
        this.user = user;
        this.achievement = achievement;
    }

    @Getter
    @Setter
    public static class HasAchievementsId implements Serializable {
        private UUID user;
        private String achievement;

        public HasAchievementsId() {
        }

        public HasAchievementsId(UUID user, String achievement) {
            this.user = user;
            this.achievement = achievement;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HasAchievementsId that = (HasAchievementsId) o;
            return Objects.equals(user, that.user) && Objects.equals(achievement, that.achievement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user, achievement);
        }
    }
}

