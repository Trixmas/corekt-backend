package lab.corekt.backend.modules.project.utils;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectUtils {
    private final ProjectRepository projectRepository;
    private final AuthContext authContext;

    public ProjectUtils(ProjectRepository projectRepository, AuthContext authContext) {
        this.projectRepository = projectRepository;
        this.authContext = authContext;
    }

    public ProjectEntity resolveProject(UUID projectId) {
        UserEntity user = authContext.getCurrentUser();
        return projectRepository.findByIdAndOwner(projectId, user).orElse(null);
    }
}
