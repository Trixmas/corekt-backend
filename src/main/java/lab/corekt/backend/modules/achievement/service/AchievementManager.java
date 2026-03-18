package lab.corekt.backend.modules.achievement.service;

import lab.corekt.backend.modules.achievement.criteria.dto.AchievementResponse;
import lab.corekt.backend.modules.achievement.criteria.dto.UserAchievementResponse;
import lab.corekt.backend.modules.achievement.entity.AchievementEntity;
import lab.corekt.backend.modules.achievement.entity.UserAchievementEntity;
import lab.corekt.backend.modules.achievement.repository.AchievementRepository;
import lab.corekt.backend.modules.achievement.repository.UserAchievementRepository;
import lab.corekt.backend.modules.achievement.trigger.TriggerContext;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementManager {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final AuthContext authContext;
    private final CriteriaEvaluator criteriaEvaluator;

    @Transactional
    public List<AchievementEntity> evaluate(TriggerContext context) {
        UserEntity currentUser = authContext.getCurrentUser();

        return achievementRepository.findAll().stream()
            .filter(achievement -> !userAchievementRepository.existsByUserAndAchievementId(currentUser, achievement.getId()))
            .filter(achievement -> criteriaEvaluator.isSatisfied(achievement, context))
            .map(achievement -> grant(currentUser, achievement))
            .toList();
    }

    private AchievementEntity grant(UserEntity user, AchievementEntity achievement) {
        userAchievementRepository.save(new UserAchievementEntity(user, achievement));
        return achievement;
    }

    @Transactional(readOnly = true)
    public List<AchievementResponse> getAllAchievements() {
        UserEntity currentUser = authContext.getCurrentUser();

        return achievementRepository.findAll().stream()
            .filter(achievement -> !achievement.isHidden() ||
                userAchievementRepository.existsByUserAndAchievementId(currentUser, achievement.getId()))
            .map(AchievementResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<UserAchievementResponse> getUserAchievements() {
        UserEntity currentUser = authContext.getCurrentUser();

        return userAchievementRepository.findByUser(currentUser).stream()
            .map(UserAchievementResponse::from)
            .toList();
    }
}
