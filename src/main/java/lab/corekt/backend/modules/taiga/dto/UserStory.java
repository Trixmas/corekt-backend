package lab.corekt.backend.modules.taiga.dto;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;

public record UserStory(String title, String description) {

    private static final String TEMPLATE_TITLE_ONLY = "US: {title = %s}";
    private static final String TEMPLATE_FULL = "US: {title = %s, description = %s}";

    public UserStory(String title, String description) {
        this.title = title;
        this.description = description != null ? description : "";
    }

    @Override
    public String toString() {
        String content = description.isBlank()
            ? TEMPLATE_TITLE_ONLY.formatted(title)
            : TEMPLATE_FULL.formatted(title, description);
        return content.substring(0, Math.min(content.length(), ParsedDataEntity.MAX_CONTENT_LENGTH));
    }
}

