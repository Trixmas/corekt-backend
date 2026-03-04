package lab.corekt.backend.modules.project.file.taiga.dto;

import lombok.Getter;

import java.util.Map;


@Getter
public class ProjectResponse {
    private final boolean success;
    private final String errorMessage;
    private Map<String, String> projects;

    public ProjectResponse(Map<String, String> projects) {
        this.projects = projects;
        this.success = projects != null;
        this.errorMessage = null;
    }

    public ProjectResponse(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }


}
