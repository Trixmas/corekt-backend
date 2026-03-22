package lab.corekt.backend.modules.achievement.service;

import lab.corekt.backend.modules.achievement.entity.AchievementEntity;
import lab.corekt.backend.modules.achievement.trigger.AchievementCondition;
import lab.corekt.backend.modules.achievement.trigger.AchievementTrigger;
import lab.corekt.backend.modules.achievement.trigger.TriggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriteriaEvaluatorTest {

    private CriteriaEvaluator criteriaEvaluator;

    @BeforeEach
    void setUp() {
        criteriaEvaluator = new CriteriaEvaluator();
    }

    @Test
    @DisplayName("Should return false when trigger does not match")
    void testIsSatisfiedReturnsFalseWhenTriggerDoesNotMatch() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "created_project": {
                  "trigger": "PROJECT_CREATED",
                  "conditions": {}
                }
              }
            }
            """);

        TriggerContext context = TriggerContext.of(
            AchievementTrigger.ANALYSIS_COMPLETED,
            AchievementCondition.score(90)
        );

        assertThat(criteriaEvaluator.isSatisfied(achievement, context)).isFalse();
    }

    @Test
    @DisplayName("Should return true when trigger matches and no conditions are defined")
    void testIsSatisfiedReturnsTrueWhenTriggerMatchesAndNoConditions() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "any_analysis": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": {}
                }
              }
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(50, 1))).isTrue();
    }

    @Test
    @DisplayName("Should return true when score is above minimum")
    void testIsSatisfiedReturnsTrueWhenScoreAboveMin() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "high_score": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": {
                    "score": { "min": 80 }
                  }
                }
              }
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(85, 1))).isTrue();
    }

    @Test
    @DisplayName("Should return false when score is below minimum")
    void testIsSatisfiedReturnsFalseWhenScoreBelowMin() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "high_score": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": {
                    "score": { "min": 80 }
                  }
                }
              }
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(79, 1))).isFalse();
    }

    @Test
    @DisplayName("Should return true when score is exactly at minimum boundary")
    void testIsSatisfiedReturnsTrueWhenScoreExactlyAtMin() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "high_score": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": {
                    "score": { "min": 80 }
                  }
                }
              }
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(80, 1))).isTrue();
    }

    @Test
    @DisplayName("Should return true when score is within min and max range")
    void testIsSatisfiedReturnsTrueWhenScoreWithinRange() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "mid_range": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": {
                    "score": { "min": 40, "max": 79 }
                  }
                }
              }
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(60, 1))).isTrue();
    }

    @Test
    @DisplayName("Should return false when score exceeds maximum")
    void testIsSatisfiedReturnsFalseWhenScoreAboveMax() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "mid_range": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": {
                    "score": { "min": 40, "max": 79 }
                  }
                }
              }
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(80, 1))).isFalse();
    }

    @Test
    @DisplayName("Should return true when all conditions are met")
    void testIsSatisfiedReturnsTrueWhenAllConditionsMet() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "veteran_high_score": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": {
                    "score": { "min": 80 },
                    "total_analyses_count": { "min": 5 }
                  }
                }
              }
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(85, 6))).isTrue();
    }

    @Test
    @DisplayName("Should return false when at least one condition is not met")
    void testIsSatisfiedReturnsFalseWhenOneConditionNotMet() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "veteran_high_score": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": {
                    "score": { "min": 80 },
                    "total_analyses_count": { "min": 5 }
                  }
                }
              }
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(85, 3))).isFalse();
    }

    @Test
    @DisplayName("Should return true when all requirement sub-lists are satisfied (AND logic)")
    void testIsSatisfiedReturnsTrueWhenRequirementsAndLogicSatisfied() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "five_analyses": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": { "total_analyses_count": { "min": 5 } }
                },
                "high_score": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": { "score": { "min": 80 } }
                }
              },
              "requirements": [["five_analyses"], ["high_score"]]
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(85, 6))).isTrue();
    }

    @Test
    @DisplayName("Should return false when one requirement sub-list is not satisfied (AND logic)")
    void testIsSatisfiedReturnsFalseWhenOneRequirementSubListNotSatisfied() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "five_analyses": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": { "total_analyses_count": { "min": 5 } }
                },
                "high_score": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": { "score": { "min": 80 } }
                }
              },
              "requirements": [["five_analyses"], ["high_score"]]
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(85, 3))).isFalse();
    }

    @Test
    @DisplayName("Should return true when at least one criterion in a sub-list is satisfied (OR logic)")
    void testIsSatisfiedReturnsTrueWhenOrLogicWithinSubListSatisfied() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "high_score": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": { "score": { "min": 80 } }
                },
                "many_analyses": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": { "total_analyses_count": { "min": 10 } }
                }
              },
              "requirements": [["high_score", "many_analyses"]]
            }
            """);

        assertThat(criteriaEvaluator.isSatisfied(achievement, analysisContext(70, 10))).isTrue();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when condition key is unknown")
    void testIsSatisfiedThrowsExceptionWhenConditionKeyIsUnknown() {
        AchievementEntity achievement = achievementWith("""
            {
              "criteria": {
                "unknown_condition": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": {
                    "unknown_key": { "min": 1 }
                  }
                }
              }
            }
            """);

        TriggerContext context = TriggerContext.of(AchievementTrigger.ANALYSIS_COMPLETED,
            AchievementCondition.score(80)
        );

        assertThatThrownBy(() -> criteriaEvaluator.isSatisfied(achievement, context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unknown_key");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when criteria JSON is invalid")
    void testIsSatisfiedThrowsExceptionWhenCriteriaJsonIsInvalid() {
        AchievementEntity achievement = achievementWith("not valid json {{{");

        assertThatThrownBy(() -> criteriaEvaluator.isSatisfied(achievement, analysisContext(80, 1)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to parse achievement criteria");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AchievementEntity achievementWith(String criteria) {
        return new AchievementEntity("test_id", "Test", "Test description", false, criteria);
    }

    private TriggerContext analysisContext(int score, int totalAnalysesCount) {
        return TriggerContext.of(AchievementTrigger.ANALYSIS_COMPLETED,
            AchievementCondition.score(score),
            AchievementCondition.totalAnalysesCount(totalAnalysesCount)
        );
    }
}
