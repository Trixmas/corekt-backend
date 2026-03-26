package lab.corekt.backend.modules.achievement.trigger;

public record AchievementCondition(String key, int value) {

    public static AchievementCondition score(int value) {
        return new AchievementCondition("score", value);
    }

    public static AchievementCondition totalAnalysesCount(int value) {
        return new AchievementCondition("total_analyses_count", value);
    }

    public static AchievementCondition totalProjectsCount(int value) {
        return new AchievementCondition("total_projects_count", value);
    }

    public static AchievementCondition totalProjectsWithPerfectAnalysis(int value) {
        return new AchievementCondition("total_perfect_projects_count", value);
    }

    public static AchievementCondition totalProjectWithPerfectAnalysisAtFirst(int value) {
        return new AchievementCondition("total_perfect_projects_first", value);
    }

    public static AchievementCondition totalProjectWithPerfectAnalysisAtLast(int value) {
        return new AchievementCondition("total_perfect_projects_last", value);
    }
}
