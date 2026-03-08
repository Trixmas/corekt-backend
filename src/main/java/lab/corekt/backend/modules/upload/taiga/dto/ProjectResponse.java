package lab.corekt.backend.modules.upload.taiga.dto;

import lombok.Getter;

import java.util.Map;


@Getter
public class ProjectResponse {
    private final boolean success;
    private String errorMessage = null;
    private Map<String, String> projects;

    public ProjectResponse(Map<String, String> projects) {
        this.projects = projects;
        this.success = projects != null;

    }

    public ProjectResponse(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }


}
