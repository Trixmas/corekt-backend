package lab.corekt.backend.modules.project.utils;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectService {
    private static ProjectRepository projectRepository;
    private static AuthContext authContext;

    ProjectService(ProjectRepository projectRepository, AuthContext authContext) {
        ProjectService.projectRepository = projectRepository;
        ProjectService.authContext = authContext;
    }

    public static ProjectEntity resolveProject(UUID projectId) {
        UserEntity user = ProjectService.authContext.getCurrentUser();
        return ProjectService.projectRepository.findByIdAndOwner(projectId, user).orElse(null);
    }
}
