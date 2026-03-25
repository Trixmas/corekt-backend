package lab.corekt.backend.modules.achievement.criteria;

import lab.corekt.backend.modules.achievement.trigger.AchievementTrigger;

import java.util.Map;

public record CriterionDefinition(
    AchievementTrigger trigger,
    Map<String, ConditionRange> conditions
) {
}
