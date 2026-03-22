package lab.corekt.backend.modules.project.service;

import lab.corekt.backend.modules.project.dto.CreateProjectRequest;
import lab.corekt.backend.modules.project.dto.ProjectResponse;
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
    public ProjectResponse createProject(CreateProjectRequest request, UserEntity owner) {
        ProjectEntity newProject = new ProjectEntity(request.getTitle(), request.getDescription(), owner);
        return new ProjectResponse(projectRepository.save(newProject));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllOwnerProjects(UserEntity owner) {
        return projectRepository.findByOwner(owner)
            .stream()
            .map(ProjectResponse::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getOwnersProjectById(UUID projectId, UserEntity owner) {
        return projectRepository.findByIdAndOwner(projectId, owner)
            .map(ProjectResponse::new)
            .orElse(null);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, UserEntity currentUser) {
        ProjectEntity project = projectRepository.findByIdAndOwner(projectId, currentUser)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (request.getTitle() != null) project.setTitle(request.getTitle());
        if (request.getDescription() != null) project.setDescription(request.getDescription());

        return new ProjectResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(UUID projectId, UserEntity currentUser) {
        ProjectEntity project = projectRepository.findByIdAndOwner(projectId, currentUser)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public int countByOwner(UserEntity owner) {
        return (int) projectRepository.countByOwner(owner);
    }
}
