package lab.corekt.backend.modules.achievement.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

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

    @Column
    private LocalDate acquiredDate;

    public HasAchievementsEntity() {
    }

    public HasAchievementsEntity(UserEntity user, AchievementEntity achievement) {
        this.user = user;
        this.achievement = achievement;
        this.acquiredDate = LocalDate.now();
    }


    @Getter
    @Setter
    public static class HasAchievementsId implements Serializable {
        private String user; // Must match property name in HasAchievementsEntity, but type is the ID type of UserEntity (email is String)

        private String achievement; // Must match property name in HasAchievementsEntity, but type is the ID type of AchievementEntity

        public HasAchievementsId() {
        }

        public HasAchievementsId(String user, String achievement) {
            this.user = user;
            this.achievement = achievement;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HasAchievementsId that = (HasAchievementsId) o;
            return Objects.equals(user, that.user) && Objects.equals(achievement, that.achievement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user, achievement);
        }
    }
}

