package lab.corekt.backend.modules.achievement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "achievement")
@EntityListeners(AuditingEntityListener.class)
public class AchievementEntity {

    @Id
    @Column(name = "achievement_name", length = 50)
    private String achievementName;

    @Column(length = 50)
    private String achievementDescription;

    @Column(columnDefinition = "json")
    private String achievementRequirements;

    @OneToMany(mappedBy = "achievement")
    private List<HasAchievementsEntity> achievements;

    // Constructeurs
    public AchievementEntity() {
    }

    public AchievementEntity(String achievementName, String achievementDescription) {
        this.achievementName = achievementName;
        this.achievementDescription = achievementDescription;
    }
}

