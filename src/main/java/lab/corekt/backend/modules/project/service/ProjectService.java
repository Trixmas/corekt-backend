package lab.corekt.backend.modules.project.service;

import lab.corekt.backend.modules.project.dto.CreateProjectRequest;
import lab.corekt.backend.modules.project.dto.UpdateProjectRequest;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectEntity createProject(CreateProjectRequest request, UserEntity owner) {
        ProjectEntity newProject = new ProjectEntity(request.getTitle(), request.getDescription(), owner);
        return projectRepository.save(newProject);
    }

    @Transactional
    public List<ProjectEntity> getAllOwnersProjects(UserEntity owner) {
        return projectRepository.findByOwner(owner);
    }

    @Transactional
    public ProjectEntity getOwnersProjectById(UUID projectId, UserEntity owner) {
        return projectRepository.findByIdAndOwner(projectId, owner).orElse(null);
    }

    @Transactional
    public ProjectEntity updateProject(UUID projectId, UpdateProjectRequest request, UserEntity currentUser) throws ResponseStatusException {
        ProjectEntity project = projectRepository.findByIdAndOwner(projectId, currentUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (request.getTitle() != null) {
            project.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        return projectRepository.save(project);
    }
}
