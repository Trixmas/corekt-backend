package lab.corekt.backend.modules.project.file.taiga.dto;

import java.util.Map;

public class ProjectResponse {
    private Map<String, String> projects;

    public ProjectResponse(Map<String, String> projects) {
        this.projects = projects;
    }

    public ProjectResponse() {
    }

    public Map<String, String> getProjects() {
        return projects;
    }
}
