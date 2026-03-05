package lab.corekt.backend.modules.project.project.service;

import lab.corekt.backend.modules.project.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.project.repository.ProjectRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectEntity createProject(String projectTitle, UserEntity owner) {

        ProjectEntity newProject = new ProjectEntity(projectTitle, owner);
        return projectRepository.save(newProject);
    }

    @Transactional
    public List<ProjectEntity> getAllOwnersProjects(UserEntity owner) {

        return projectRepository.findByOwner(owner);
    }

    @Transactional
    public ProjectEntity getProjectById(UUID projectId) {

        return projectRepository.findById(projectId).orElse(null);
    }
}
