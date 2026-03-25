package lab.corekt.backend.modules.achievement.repository;

import lab.corekt.backend.modules.achievement.entity.UserAchievementEntity;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievementEntity, UserAchievementEntity.UserAchievementId> {

    List<UserAchievementEntity> findByUser(UserEntity user);

    boolean existsByUserAndAchievementId(UserEntity user, String achievementId);
}
