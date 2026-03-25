package lab.corekt.backend.modules.achievement.repository;

import lab.corekt.backend.modules.achievement.entity.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AchievementRepository extends JpaRepository<AchievementEntity, String> {
}
