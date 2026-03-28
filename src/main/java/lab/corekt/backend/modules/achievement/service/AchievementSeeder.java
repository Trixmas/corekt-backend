package lab.corekt.backend.modules.achievement.service;

import lab.corekt.backend.modules.achievement.entity.AchievementEntity;
import lab.corekt.backend.modules.achievement.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementSeeder implements ApplicationRunner {

    private final AchievementRepository achievementRepository;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        List<AchievementEntity> achievements = buildAchievements();
        achievements.stream()
            .filter(achievement -> !achievementRepository.existsById(achievement.getId()))
            .forEach(achievement -> {
                achievementRepository.save(achievement);
                log.info("Seeded achievement: {}", achievement.getId());
            });
    }

    private List<AchievementEntity> buildAchievements() {
        return List.of(
            new AchievementEntity(
                "first_project",
                "Getting Started",
                "Create your first project.",
                false,
                """
                    {
                      "criteria": {
                        "created_first_project": {
                          "trigger": "PROJECT_CREATED",
                          "conditions": {
                            "total_projects_count": { "min": 1 }
                          }
                        }
                      },
                      "requirements": [["created_first_project"]]
                    }
                    """
            ),
            new AchievementEntity(
                "project_count_10",
                "Making it a Habit",
                "Create 10 projects.",
                false,
                """
                    {
                      "criteria": {
                        "created_ten_projects": {
                          "trigger": "PROJECT_CREATED",
                          "conditions": {
                            "total_projects_count": { "min": 10 }
                          }
                        }
                      },
                      "requirements": [["created_ten_projects"]]
                    }
                    """
            ),
            new AchievementEntity(
                "project_count_25",
                "Power User",
                "Create 25 projects.",
                false,
                """
                    {
                      "criteria": {
                        "created_twenty_five_projects": {
                          "trigger": "PROJECT_CREATED",
                          "conditions": {
                            "total_projects_count": { "min": 25 }
                          }
                        }
                      },
                      "requirements": [["created_twenty_five_projects"]]
                    }
                    """
            ),
            new AchievementEntity(
                "project_count_50",
                "Part of the Routine",
                "Create 50 projects.",
                false,
                """
                    {
                      "criteria": {
                        "created_fifty_projects": {
                          "trigger": "PROJECT_CREATED",
                          "conditions": {
                            "total_projects_count": { "min": 50 }
                          }
                        }
                      },
                      "requirements": [["created_fifty_projects"]]
                    }
                    """
            ),
            new AchievementEntity(
                "project_count_100",
                "The Reference",
                "Create 100 projects.",
                true,
                """
                    {
                      "criteria": {
                        "created_one_hundred_projects": {
                          "trigger": "PROJECT_CREATED",
                          "conditions": {
                            "total_projects_count": { "min": 100 }
                          }
                        }
                      },
                      "requirements": [["created_one_hundred_projects"]]
                    }
                    """
            )
        );
    }
}
