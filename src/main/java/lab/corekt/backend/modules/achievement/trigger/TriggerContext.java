package lab.corekt.backend.modules.achievement.trigger;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public record TriggerContext(
    AchievementTrigger trigger,
    Map<String, Integer> values
) {
    public static TriggerContext of(AchievementTrigger trigger, AchievementCondition... conditions) {
        Map<String, Integer> values = Arrays.stream(conditions)
            .collect(Collectors.toMap(AchievementCondition::key, AchievementCondition::value));
        return new TriggerContext(trigger, Map.copyOf(values));
    }
}
