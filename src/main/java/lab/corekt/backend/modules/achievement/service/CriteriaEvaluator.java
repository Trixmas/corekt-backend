package lab.corekt.backend.modules.achievement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lab.corekt.backend.modules.achievement.criteria.AchievementCriteria;
import lab.corekt.backend.modules.achievement.criteria.CriterionDefinition;
import lab.corekt.backend.modules.achievement.trigger.TriggerContext;
import lab.corekt.backend.modules.achievement.entity.AchievementEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CriteriaEvaluator {
    private final ObjectMapper criteriaObjectMapper;

    public CriteriaEvaluator() {
        this.criteriaObjectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();
    }

    public boolean isSatisfied(AchievementEntity achievement, TriggerContext context) {
        AchievementCriteria parsed = deserialize(achievement.getCriteria());
        List<List<String>> requirements = resolveRequirements(parsed);

        return requirements.stream().allMatch(subList ->
            subList.stream().anyMatch(criterionName ->
                evaluate(parsed.criteria().get(criterionName), context)
            )
        );
    }

    private boolean evaluate(CriterionDefinition criterion, TriggerContext context) {
        if (criterion.trigger() != context.trigger()) return false;

        return criterion.conditions().entrySet().stream().allMatch(entry -> {
            Integer value = context.values().get(entry.getKey());
            if (value == null) throw new IllegalArgumentException("Unknown condition key: " + entry.getKey());
            return entry.getValue().contains(value);
        });
    }

    private List<List<String>> resolveRequirements(AchievementCriteria parsed) {
        if (parsed.requirements() != null && !parsed.requirements().isEmpty()) {
            return parsed.requirements();
        }
        return parsed.criteria().keySet().stream().map(List::of).toList();
    }

    private AchievementCriteria deserialize(String json) {
        try {
            return criteriaObjectMapper.readValue(json, AchievementCriteria.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse achievement criteria: " + json, e);
        }
    }
}
