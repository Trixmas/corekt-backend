package lab.corekt.backend.modules.achievement.criteria.dto;

import lab.corekt.backend.modules.achievement.entity.UserAchievementEntity;

import java.time.Instant;

public record UserAchievementResponse(
    String id,
    String name,
    String description,
    Instant acquiredAt
) {
    public static UserAchievementResponse from(UserAchievementEntity userAchievement) {
        return new UserAchievementResponse(
            userAchievement.getAchievement().getId(),
            userAchievement.getAchievement().getName(),
            userAchievement.getAchievement().getDescription(),
            userAchievement.getAcquiredAt()
        );
    }
}
