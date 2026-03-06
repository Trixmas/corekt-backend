package lab.corekt.backend.modules.upload.taiga.dto;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class US {
    private static final String TO_STRING_TEMPLATE = "US: {title = %s, description = %s}";
    private String title;
    private String description;

    public US(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public String toString() {
        String content = formatContent();
        return content.substring(0, Math.min(content.length(), ParsedDataEntity.MAX_CONTENT_LENGTH));
    }

    private String formatContent() {
        return TO_STRING_TEMPLATE.formatted(title, description);
    }


}
