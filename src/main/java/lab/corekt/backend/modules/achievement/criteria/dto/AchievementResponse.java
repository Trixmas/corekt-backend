package lab.corekt.backend.modules.achievement.criteria.dto;

import lab.corekt.backend.modules.achievement.entity.AchievementEntity;

public record AchievementResponse(
    String id,
    String name,
    String description,
    boolean hidden
) {
    public static AchievementResponse from(AchievementEntity achievement) {
        return new AchievementResponse(
            achievement.getId(),
            achievement.getName(),
            achievement.getDescription(),
            achievement.isHidden()
        );
    }
}
