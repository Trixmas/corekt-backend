package lab.corekt.backend.modules.achievement.service;

import lab.corekt.backend.modules.achievement.criteria.dto.AchievementResponse;
import lab.corekt.backend.modules.achievement.criteria.dto.UserAchievementResponse;
import lab.corekt.backend.modules.achievement.entity.AchievementEntity;
import lab.corekt.backend.modules.achievement.repository.AchievementRepository;
import lab.corekt.backend.modules.achievement.repository.UserAchievementRepository;
import lab.corekt.backend.modules.achievement.trigger.AchievementCondition;
import lab.corekt.backend.modules.achievement.trigger.AchievementTrigger;
import lab.corekt.backend.modules.achievement.trigger.TriggerContext;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AchievementManagerTest {

    @Autowired
    private AchievementManager achievementManager;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private AuthContext authContext;

    private UserEntity currentUser;
    private UserEntity otherUser;

    @BeforeEach
    void setUp() {
        userAchievementRepository.deleteAll();
        achievementRepository.deleteAll();
        userRepository.deleteAll();

        currentUser = userRepository.save(new UserEntity("current@test.com", "currentUser"));
        otherUser = userRepository.save(new UserEntity("other@test.com", "otherUser"));

        when(authContext.getCurrentUser()).thenReturn(currentUser);
    }

    private String criteriaForMinScore(int minScore) {
        return """
            {
              "criteria": {
                "score_check": {
                  "trigger": "ANALYSIS_COMPLETED",
                  "conditions": { "score": { "min": %d } }
                }
              }
            }
            """.formatted(minScore);
    }

    private String criteriaForProjectCreated() {
        return """
            {
              "criteria": {
                "project_check": {
                  "trigger": "PROJECT_CREATED",
                  "conditions": {}
                }
              }
            }
            """;
    }

    private TriggerContext analysisContext(int score) {
        return TriggerContext.of(AchievementTrigger.ANALYSIS_COMPLETED,
            AchievementCondition.score(score)
        );
    }

    @Test
    @DisplayName("Should grant achievement when criteria are satisfied")
    void testEvaluateGrantsAchievementWhenCriteriaSatisfied() {
        saveVisibleAchievement("score_over_80", criteriaForMinScore(80));

        List<String> unlocked = achievementManager.evaluate(analysisContext(85));

        assertThat(unlocked).containsExactly("score_over_80");
    }

    @Test
    @DisplayName("Should not grant achievement when criteria are not satisfied")
    void testEvaluateDoesNotGrantAchievementWhenCriteriaNotSatisfied() {
        saveVisibleAchievement("score_over_80", criteriaForMinScore(80));

        List<String> unlocked = achievementManager.evaluate(analysisContext(70));

        assertThat(unlocked).isEmpty();
    }

    @Test
    @DisplayName("Should grant multiple achievements when multiple criteria are satisfied")
    void testEvaluateGrantsMultipleAchievementsWhenMultipleCriteriaSatisfied() {
        saveVisibleAchievement("score_over_50", criteriaForMinScore(50));
        saveVisibleAchievement("score_over_80", criteriaForMinScore(80));

        List<String> unlocked = achievementManager.evaluate(analysisContext(85));

        assertThat(unlocked).containsExactlyInAnyOrder("score_over_50", "score_over_80");
    }

    @Test
    @DisplayName("Should not grant achievement from a different trigger")
    void testEvaluateDoesNotGrantAchievementFromDifferentTrigger() {
        saveVisibleAchievement("first_project", criteriaForProjectCreated());

        List<String> unlocked = achievementManager.evaluate(analysisContext(90));

        assertThat(unlocked).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when no achievements exist")
    void testEvaluateReturnsEmptyWhenNoAchievementsExist() {
        List<String> unlocked = achievementManager.evaluate(analysisContext(85));

        assertThat(unlocked).isEmpty();
    }

    @Test
    @DisplayName("Should not grant same achievement twice to the same user")
    void testEvaluateDoesNotGrantSameAchievementTwice() {
        saveVisibleAchievement("score_over_80", criteriaForMinScore(80));

        achievementManager.evaluate(analysisContext(85));
        List<String> secondEvaluation = achievementManager.evaluate(analysisContext(85));

        assertThat(secondEvaluation).isEmpty();
        assertThat(userAchievementRepository.findByUser(currentUser)).hasSize(1);
    }

    @Test
    @DisplayName("Should grant achievement to other user independently")
    void testEvaluateGrantsAchievementToOtherUserIndependently() {
        saveVisibleAchievement("score_over_80", criteriaForMinScore(80));

        achievementManager.evaluate(analysisContext(85));

        when(authContext.getCurrentUser()).thenReturn(otherUser);
        List<String> otherUserUnlocked = achievementManager.evaluate(analysisContext(85));

        assertThat(otherUserUnlocked).containsExactly("score_over_80");
    }

    @Test
    @DisplayName("Should return all visible achievements")
    void testGetAllAchievementsReturnsAllVisibleAchievements() {
        saveVisibleAchievement("score_over_50", criteriaForMinScore(50));
        saveVisibleAchievement("score_over_80", criteriaForMinScore(80));

        List<AchievementResponse> achievements = achievementManager.getAllAchievements();

        assertThat(achievements).hasSize(2);
        assertThat(achievements).extracting(AchievementResponse::id)
            .containsExactlyInAnyOrder("score_over_50", "score_over_80");
    }

    @Test
    @DisplayName("Should not return hidden achievement when not unlocked by current user")
    void testGetAllAchievementsDoesNotReturnHiddenAchievementWhenNotUnlocked() {
        saveHiddenAchievement("secret", criteriaForMinScore(100));

        List<AchievementResponse> achievements = achievementManager.getAllAchievements();

        assertThat(achievements).isEmpty();
    }

    @Test
    @DisplayName("Should return hidden achievement when unlocked by current user")
    void testGetAllAchievementsReturnsHiddenAchievementWhenUnlocked() {
        saveHiddenAchievement("secret", criteriaForMinScore(100));

        achievementManager.evaluate(analysisContext(100));
        List<AchievementResponse> achievements = achievementManager.getAllAchievements();

        assertThat(achievements).hasSize(1);
        assertThat(achievements.getFirst().id()).isEqualTo("secret");
    }

    @Test
    @DisplayName("Should return hidden achievement for unlocked user but not for other user")
    void testGetAllAchievementsReturnsHiddenOnlyForUserWhoUnlocked() {
        saveHiddenAchievement("secret", criteriaForMinScore(100));

        achievementManager.evaluate(analysisContext(100));

        when(authContext.getCurrentUser()).thenReturn(otherUser);
        List<AchievementResponse> otherUserAchievements = achievementManager.getAllAchievements();

        assertThat(otherUserAchievements).isEmpty();
    }

    @Test
    @DisplayName("Should return only achievements unlocked by current user")
    void testGetUserAchievementsReturnsOnlyCurrentUserAchievements() {
        saveVisibleAchievement("score_over_80", criteriaForMinScore(80));

        achievementManager.evaluate(analysisContext(85));
        List<UserAchievementResponse> userAchievements = achievementManager.getUserAchievements();

        assertThat(userAchievements).hasSize(1);
        assertThat(userAchievements.getFirst().id()).isEqualTo("score_over_80");
    }

    @Test
    @DisplayName("Should return empty list when current user has no unlocked achievements")
    void testGetUserAchievementsReturnsEmptyWhenNoneUnlocked() {
        saveVisibleAchievement("score_over_80", criteriaForMinScore(80));

        List<UserAchievementResponse> userAchievements = achievementManager.getUserAchievements();

        assertThat(userAchievements).isEmpty();
    }

    @Test
    @DisplayName("Should not return achievements unlocked by another user")
    void testGetUserAchievementsDoesNotReturnOtherUsersAchievements() {
        saveVisibleAchievement("score_over_80", criteriaForMinScore(80));

        when(authContext.getCurrentUser()).thenReturn(otherUser);
        achievementManager.evaluate(analysisContext(85));

        when(authContext.getCurrentUser()).thenReturn(currentUser);
        List<UserAchievementResponse> currentUserAchievements = achievementManager.getUserAchievements();

        assertThat(currentUserAchievements).isEmpty();
    }

    @Test
    @DisplayName("Should return acquired date when achievement is unlocked")
    void testGetUserAchievementsReturnsAcquiredDate() {
        saveVisibleAchievement("score_over_80", criteriaForMinScore(80));

        achievementManager.evaluate(analysisContext(85));
        List<UserAchievementResponse> userAchievements = achievementManager.getUserAchievements();

        assertThat(userAchievements.getFirst().acquiredAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AchievementEntity saveAchievement(String id, String criteria, boolean hidden) {
        return achievementRepository.save(new AchievementEntity(id, id, "Description for " + id, hidden, criteria));
    }

    private AchievementEntity saveVisibleAchievement(String id, String criteria) {
        return saveAchievement(id, criteria, false);
    }

    private AchievementEntity saveHiddenAchievement(String id, String criteria) {
        return saveAchievement(id, criteria, true);
    }
}
