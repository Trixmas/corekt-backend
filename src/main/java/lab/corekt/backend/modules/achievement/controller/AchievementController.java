package lab.corekt.backend.modules.achievement.controller;

import lab.corekt.backend.modules.achievement.criteria.dto.AchievementResponse;
import lab.corekt.backend.modules.achievement.criteria.dto.UserAchievementResponse;
import lab.corekt.backend.modules.achievement.service.AchievementManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementManager achievementManager;

    @GetMapping
    public ResponseEntity<List<AchievementResponse>> getAllAchievements() {
        return ResponseEntity.ok(achievementManager.getAllAchievements());
    }

    @GetMapping("/me")
    public ResponseEntity<List<UserAchievementResponse>> getUserAchievements() {
        return ResponseEntity.ok(achievementManager.getUserAchievements());
    }
}
