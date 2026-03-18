package lab.corekt.backend.modules.achievement.criteria;

import java.util.List;
import java.util.Map;

public record AchievementCriteria(
    Map<String, CriterionDefinition> criteria,
    List<List<String>> requirements
) {
}
