package lab.corekt.backend.modules.achievement.criteria;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConditionRange(
    @JsonProperty("min") Integer min,
    @JsonProperty("max") Integer max
) {
    public boolean contains(int value) {
        if (min != null && value < min) return false;
        if (max != null && value > max) return false;
        return true;
    }
}
