package lab.corekt.backend.modules.project.file.taiga.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class US {
    private String title;
    private String description;

    public US(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
