package lab.corekt.backend.modules.achievement.service.helper;

import lab.corekt.backend.modules.achievement.service.AchievementManager;
import lab.corekt.backend.modules.achievement.trigger.AchievementCondition;
import lab.corekt.backend.modules.achievement.trigger.TriggerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wraps controller responses with achievement evaluation.
 *
 * <p>Use {@link #ok} or {@link #created} instead of {@code ResponseEntity.ok()} / {@code ResponseEntity.status(CREATED)}
 * on endpoints that can unlock achievements. Unlocked achievement IDs are returned in the
 * {@code X-Unlocked-Achievements} response header — the body is left unchanged.
 *
 * <p>To add a new achievement: insert a row in the {@code achievement} table with a valid
 * JSONB {@code criteria} block. No code changes are required.
 *
 * @see AchievementManager
 * @see TriggerContext
 * @see AchievementCondition
 */
@Component
@RequiredArgsConstructor
public class AchievementResponseHelper {

    private final AchievementManager achievementManager;

    /** Returns {@code 200 OK} with unlocked achievement IDs in the {@code X-Unlocked-Achievements} header. */
    public <T> ResponseEntity<T> ok(T body, TriggerContext context) {
        List<String> unlocked = achievementManager.evaluate(context);
        return ResponseEntity.ok()
            .header("X-Unlocked-Achievements", String.join(",", unlocked))
            .body(body);
    }

    /** Returns {@code 201 Created} with unlocked achievement IDs in the {@code X-Unlocked-Achievements} header. */
    public <T> ResponseEntity<T> created(T body, TriggerContext context) {
        List<String> unlocked = achievementManager.evaluate(context);
        return ResponseEntity.status(HttpStatus.CREATED)
            .header("X-Unlocked-Achievements", String.join(",", unlocked))
            .body(body);
    }
}
