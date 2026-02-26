package lab.corekt.backend.modules.achievement.repository;

import lab.corekt.backend.modules.achievement.entity.HasAchievementsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HasAchievementsRepository extends JpaRepository<HasAchievementsEntity, HasAchievementsEntity.HasAchievementsId> {
    List<HasAchievementsEntity> findByUser_Email(String email);

    List<HasAchievementsEntity> findByAchievement_AchievementName(String achievementName);
}

