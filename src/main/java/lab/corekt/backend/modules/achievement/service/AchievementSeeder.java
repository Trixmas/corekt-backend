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
            ),
            new AchievementEntity(
                    "first_analysis",
                    "First Insight",
                    "Complete your very first functional analysis.",
                    false,
                    """
                        {
                          "criteria": {
                            "completed_first_analysis": {
                              "trigger": "ANALYSIS_COMPLETED",
                              "conditions": {
                                "total_analyses_count": { "min": 1 }
                              }
                            }
                          },
                          "requirements": [["completed_first_analysis"]]
                        }
                        """
            ),
            new AchievementEntity(
                    "first_perfect_analysis",
                    "Surgical Precision",
                    "Reach a score above 90%",
                    false,
                    """
                        {
                          "criteria": {
                            "complete_first_perfect_analysis": {
                              "trigger": "ANALYSIS_COMPLETED",
                              "conditions": {
                                "total_perfect_projects_count": { "min": 1 }
                              }
                            }
                          },
                          "requirements": [["complete_first_perfect_analysis"]]
                        }
                        """
            ),
            new AchievementEntity(
                    "perfect_analysis_count_10",
                    "Consistent Excellence",
                    "Reach a score above 90% on 10 different projects.",
                    false,
                    """
                        {
                          "criteria": {
                            "complete_ten_perfect_analyses": {
                              "trigger": "ANALYSIS_COMPLETED",
                              "conditions": {
                                "total_perfect_projects_count": { "min": 10 }
                              }
                            }
                          },
                          "requirements": [["complete_ten_perfect_analyses"]]
                        }
                        """
            ),
            new AchievementEntity(
                    "perfect_analysis_count_25",
                    "Quality Champion",
                    "Reach a score above 90% on 25 different projects.",
                    false,
                    """
                        {
                          "criteria": {
                            "complete_twenty_five_perfect_analyses": {
                              "trigger": "ANALYSIS_COMPLETED",
                              "conditions": {
                                "total_perfect_projects_count": { "min": 25 }
                              }
                            }
                          },
                          "requirements": [["complete_twenty_five_perfect_analyses"]]
                        }
                        """
            ),
            new AchievementEntity(
                    "perfect_analysis_count_50",
                    "Master of Compliance",
                    "Reach a score above 90% on 50 different projects.",
                    false,
                    """
                        {
                          "criteria": {
                            "complete_fifty_perfect_analyses": {
                              "trigger": "ANALYSIS_COMPLETED",
                              "conditions": {
                                "total_perfect_projects_count": { "min": 50 }
                              }
                            }
                          },
                          "requirements": [["complete_fifty_perfect_analyses"]]
                        }
                        """
            ),
            new AchievementEntity(
                    "perfect_on_first_version",
                    "First Time Right",
                    "Reach a score above 90% on the very first version of a project",
                    false,
                    """
                        {
                          "criteria": {
                            "perfect_analysis_on_first_version": {
                              "trigger": "ANALYSIS_COMPLETED",
                              "conditions": {
                                "total_perfect_projects_first": { "min": 1 }
                              }
                            }
                          },
                          "requirements": [["perfect_analysis_on_first_version"]]
                        }
                        """
            ),
            new AchievementEntity(
                    "perfect_on_last_version",
                    "Talk About Suspense!",
                    "Hit the perfect score on your very last attempt.",
                    false,
                    """
                        {
                          "criteria": {
                            "perfect_analysis_on_last_version": {
                              "trigger": "ANALYSIS_COMPLETED",
                              "conditions": {
                                "total_perfect_projects_last": { "min": 1 }
                              }
                            }
                          },
                          "requirements": [["perfect_analysis_on_last_version"]]
                        }
                        """
            )
        );
    }
}
