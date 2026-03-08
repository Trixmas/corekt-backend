package lab.corekt.backend.modules.project.service;

import lab.corekt.backend.modules.project.dto.CreateProjectRequest;
import lab.corekt.backend.modules.project.dto.UpdateProjectRequest;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private UserEntity buildUser() {
        return new UserEntity("user@test.com", "testuser");
    }

    private CreateProjectRequest buildCreateRequest(String title, String description) {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle(title);
        request.setDescription(description);
        return request;
    }

    @Test
    @DisplayName("Should create and return project when request is valid")
    void testCreatesAndReturnsProjectWhenRequestIsValid() {
        UserEntity user = buildUser();
        CreateProjectRequest request = buildCreateRequest("My Project", "A description");
        ProjectEntity savedProject = new ProjectEntity("My Project", "A description", user);

        when(projectRepository.save(any(ProjectEntity.class))).thenReturn(savedProject);

        ProjectEntity result = projectService.createProject(request, user);

        assertThat(result.getTitle()).isEqualTo("My Project");
        assertThat(result.getDescription()).isEqualTo("A description");
        assertThat(result.getOwner()).isEqualTo(user);
        verify(projectRepository).save(any(ProjectEntity.class));
    }

    @Test
    @DisplayName("Should return all projects belonging to owner")
    void testReturnsAllProjectsBelongingToOwner() {
        UserEntity user = buildUser();
        List<ProjectEntity> projects = List.of(
            new ProjectEntity("Project A", "Desc A", user),
            new ProjectEntity("Project B", "Desc B", user)
        );

        when(projectRepository.findByOwner(user)).thenReturn(projects);

        List<ProjectEntity> result = projectService.getAllOwnerProjects(user);

        assertThat(result).hasSize(2);
        verify(projectRepository).findByOwner(user);
    }

    @Test
    @DisplayName("Should return project when it belongs to owner")
    void testReturnsProjectWhenItBelongsToOwner() {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = new ProjectEntity("My Project", "A description", user);

        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.of(project));

        ProjectEntity result = projectService.getOwnersProjectById(projectId, user);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("My Project");
    }

    @Test
    @DisplayName("Should return null when project does not belong to owner")
    void testReturnsNullWhenProjectDoesNotBelongToOwner() {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();

        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.empty());

        ProjectEntity result = projectService.getOwnersProjectById(projectId, user);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should update title and description when both are provided")
    void testUpdatesTitleAndDescriptionWhenBothAreProvided() {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = new ProjectEntity("Old Title", "Old Description", user);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setTitle("New Title");
        request.setDescription("New Description");

        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        ProjectEntity result = projectService.updateProject(projectId, request, user);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getDescription()).isEqualTo("New Description");
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("Should update only title when description is null")
    void testUpdatesOnlyTitleWhenDescriptionIsNull() {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = new ProjectEntity("Old Title", "Old Description", user);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setTitle("New Title");

        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        ProjectEntity result = projectService.updateProject(projectId, request, user);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getDescription()).isEqualTo("Old Description");
    }

    @Test
    @DisplayName("Should throw 404 when updating project not owned by user")
    void testThrows404WhenUpdatingProjectNotOwnedByUser() {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();

        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.empty());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setTitle("New Title");

        assertThatThrownBy(() -> projectService.updateProject(projectId, request, user))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Should delete project when it belongs to owner")
    void testDeletesProjectWhenItBelongsToOwner() {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = new ProjectEntity("My Project", "A description", user);

        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.of(project));

        projectService.deleteProject(projectId, user);

        verify(projectRepository).delete(project);
    }

    @Test
    @DisplayName("Should throw 404 when deleting project not owned by user")
    void testThrows404WhenDeletingProjectNotOwnedByUser() {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();

        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(projectId, user))
            .isInstanceOf(ResponseStatusException.class);
    }
}
