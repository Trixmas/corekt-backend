package lab.corekt.backend.modules.achievement.trigger;

/**
 * A data container representing a specific metric used to evaluate achievement progress.
 * This record provides a set of factory methods to standardize the keys used within
 * the achievement evaluation engine, ensuring consistency between the Java code
 * and the JSON criteria stored in the database.
 * @param key   The identifier matching the JSON criteria key (e.g., "score", "total_projects_count").
 * @param value The current numeric value achieved by the user for this specific metric.
 */
public record AchievementCondition(String key, int value) {

    /**
     * Creates a condition tracking the current analysis performance.
     * @param value The 0-100 score of the specific version being evaluated.
     * @return A condition mapped to the "score" JSON key.
     */
    public static AchievementCondition score(int value) {
        return new AchievementCondition("score", value);
    }

    /**
     * Creates a condition tracking the user's total number of completed analyses.
     * @param value The lifetime count of analyses with a COMPLETED status.
     * @return A condition mapped to the "total_analyses_count" JSON key.
     */
    public static AchievementCondition totalAnalysesCount(int value) {
        return new AchievementCondition("total_analyses_count", value);
    }

    /**
     * Creates a condition tracking the total number of projects created by the user.
     * @param value The lifetime count of ProjectEntity entries owned by the user.
     * @return A condition mapped to the "total_projects_count" JSON key.
     */
    public static AchievementCondition totalProjectsCount(int value) {
        return new AchievementCondition("total_projects_count", value);
    }

    /**
     * Creates a condition tracking projects that reached the excellence threshold (>90%).
     * @param value The count of distinct projects having at least one high-score version.
     * @return A condition mapped to the "total_perfect_projects_count" JSON key.
     */
    public static AchievementCondition totalProjectsWithPerfectAnalysis(int value) {
        return new AchievementCondition("total_perfect_projects_count", value);
    }

    /**
     * Creates a condition for projects perfected on the very first version.
     * @param value The count of projects where Version 1 achieved a >90% score.
     * @return A condition mapped to the "total_perfect_projects_first" JSON key.
     */
    public static AchievementCondition totalProjectWithPerfectAnalysisAtFirst(int value) {
        return new AchievementCondition("total_perfect_projects_first", value);
    }

    /**
     * Creates a condition for projects perfected on the final attempt (Version 10).
     * @param value The count of projects where Version 10 achieved a >90% score.
     * @return A condition mapped to the "total_perfect_projects_last" JSON key.
     */
    public static AchievementCondition totalProjectWithPerfectAnalysisAtLast(int value) {
        return new AchievementCondition("total_perfect_projects_last", value);
    }
}
