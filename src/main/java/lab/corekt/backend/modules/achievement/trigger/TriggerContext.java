package lab.corekt.backend.modules.achievement.trigger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public record TriggerContext(AchievementTrigger trigger, Map<String, Integer> values) {

    public static Builder of(AchievementTrigger trigger) {
        return new Builder(trigger);
    }

    public static class Builder {
        private final AchievementTrigger trigger;
        private final Map<String, Integer> values;

        private Builder(AchievementTrigger trigger) {
            this.trigger = trigger;
            this.values = new HashMap<>();
        }

        public Builder with(AchievementCondition... conditions) {
            Arrays.stream(conditions).forEach(
                condition -> values.put(condition.key(), condition.value())
            );
            return this;
        }

        public TriggerContext build() {
            return new TriggerContext(trigger, values);
        }
    }
}
