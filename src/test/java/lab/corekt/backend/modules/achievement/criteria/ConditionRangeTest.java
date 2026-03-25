package lab.corekt.backend.modules.achievement.criteria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionRangeTest {

    @Test
    @DisplayName("Should return true when value is above minimum")
    void testContainsReturnsTrueWhenValueAboveMin() {
        assertThat(new ConditionRange(80, null).contains(85)).isTrue();
    }

    @Test
    @DisplayName("Should return true when value is exactly at minimum")
    void testContainsReturnsTrueWhenValueExactlyAtMin() {
        assertThat(new ConditionRange(80, null).contains(80)).isTrue();
    }

    @Test
    @DisplayName("Should return false when value is below minimum")
    void testContainsReturnsFalseWhenValueBelowMin() {
        assertThat(new ConditionRange(80, null).contains(79)).isFalse();
    }

    @Test
    @DisplayName("Should return true when value is below maximum")
    void testContainsReturnsTrueWhenValueBelowMax() {
        assertThat(new ConditionRange(null, 79).contains(60)).isTrue();
    }

    @Test
    @DisplayName("Should return true when value is exactly at maximum")
    void testContainsReturnsTrueWhenValueExactlyAtMax() {
        assertThat(new ConditionRange(null, 79).contains(79)).isTrue();
    }

    @Test
    @DisplayName("Should return false when value is above maximum")
    void testContainsReturnsFalseWhenValueAboveMax() {
        assertThat(new ConditionRange(null, 79).contains(80)).isFalse();
    }

    @Test
    @DisplayName("Should return true when value is within range")
    void testContainsReturnsTrueWhenValueWithinRange() {
        assertThat(new ConditionRange(40, 79).contains(60)).isTrue();
    }

    @Test
    @DisplayName("Should return true when value is exactly at minimum boundary")
    void testContainsReturnsTrueWhenValueAtMinBoundary() {
        assertThat(new ConditionRange(40, 79).contains(40)).isTrue();
    }

    @Test
    @DisplayName("Should return true when value is exactly at maximum boundary")
    void testContainsReturnsTrueWhenValueAtMaxBoundary() {
        assertThat(new ConditionRange(40, 79).contains(79)).isTrue();
    }

    @Test
    @DisplayName("Should return false when value is below range")
    void testContainsReturnsFalseWhenValueBelowRange() {
        assertThat(new ConditionRange(40, 79).contains(39)).isFalse();
    }

    @Test
    @DisplayName("Should return false when value is above range")
    void testContainsReturnsFalseWhenValueAboveRange() {
        assertThat(new ConditionRange(40, 79).contains(80)).isFalse();
    }

    @Test
    @DisplayName("Should return true for any value when both min and max are null")
    void testContainsReturnsTrueForAnyValueWhenNoBounds() {
        assertThat(new ConditionRange(null, null).contains(0)).isTrue();
        assertThat(new ConditionRange(null, null).contains(100)).isTrue();
        assertThat(new ConditionRange(null, null).contains(Integer.MAX_VALUE)).isTrue();
    }
}
