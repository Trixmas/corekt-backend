package lab.corekt.backend.modules.project.utils;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectUtils {
    private static ProjectRepository projectRepository;
    private static AuthContext authContext;

    ProjectUtils(ProjectRepository projectRepository, AuthContext authContext) {
        ProjectUtils.projectRepository = projectRepository;
        ProjectUtils.authContext = authContext;
    }

    public static ProjectEntity resolveProject(UUID projectId) {
        UserEntity user = ProjectUtils.authContext.getCurrentUser();
        return ProjectUtils.projectRepository.findByIdAndOwner(projectId, user).orElse(null);
    }
}
