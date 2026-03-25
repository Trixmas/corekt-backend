package lab.corekt.backend.modules.achievement.trigger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriggerContextTest {

    @Test
    @DisplayName("Should build context with correct trigger")
    void testOfSetsCorrectTrigger() {
        TriggerContext context = TriggerContext.of(AchievementTrigger.ANALYSIS_COMPLETED,
            AchievementCondition.score(80)
        );

        assertThat(context.trigger()).isEqualTo(AchievementTrigger.ANALYSIS_COMPLETED);
    }

    @Test
    @DisplayName("Should build context with all provided conditions")
    void testOfContainsAllProvidedConditions() {
        TriggerContext context = TriggerContext.of(
            AchievementTrigger.ANALYSIS_COMPLETED,
            AchievementCondition.score(80),
            AchievementCondition.totalAnalysesCount(5)
        );

        assertThat(context.values())
            .containsEntry("score", 80)
            .containsEntry("total_analyses_count", 5);
    }

    @Test
    @DisplayName("Should build context with no conditions when none are provided")
    void testOfBuildsEmptyValuesWhenNoConditions() {
        TriggerContext context = TriggerContext.of(AchievementTrigger.PROJECT_CREATED);

        assertThat(context.values()).isEmpty();
    }

    @Test
    @DisplayName("Should build context with correct total projects count condition")
    void testOfContainsTotalProjectsCountCondition() {
        TriggerContext context = TriggerContext.of(
            AchievementTrigger.PROJECT_CREATED,
            AchievementCondition.totalProjectsCount(3)
        );

        assertThat(context.values()).containsEntry("total_projects_count", 3);
    }

    @Test
    @DisplayName("Should return an unmodifiable values map")
    void testValuesMapIsUnmodifiable() {
        TriggerContext context = TriggerContext.of(
            AchievementTrigger.ANALYSIS_COMPLETED,
            AchievementCondition.score(80)
        );

        assertThatThrownBy(() -> context.values().put("score", 99))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
