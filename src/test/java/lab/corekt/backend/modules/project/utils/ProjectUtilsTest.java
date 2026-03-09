package lab.corekt.backend.modules.project.utils;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectUtilsTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuthContext authContext;

    private ProjectUtils projectUtils;
    private UserEntity currentUser;
    private UUID projectId;
    private ProjectEntity project;

    @BeforeEach
    void setUp() {
        projectUtils = new ProjectUtils(projectRepository, authContext);

        currentUser = new UserEntity("user@example.com", "testUser");
        currentUser.setId(UUID.randomUUID());
        when(authContext.getCurrentUser()).thenReturn(currentUser);

        projectId = UUID.randomUUID();
        project = new ProjectEntity("Test Project", "A description", currentUser);
        project.setId(projectId);
    }

    @Test
    @DisplayName("resolveProject uses the authenticated user to look up the project")
    void testResolveProjectUsesAuthenticatedUser() {
        when(projectRepository.findByIdAndOwner(projectId, currentUser))
                .thenReturn(Optional.of(project));

        ProjectEntity result = projectUtils.resolveProject(projectId);

        assertThat(result).isEqualTo(project);
        verify(authContext, times(1)).getCurrentUser();
        verify(projectRepository, times(1)).findByIdAndOwner(projectId, currentUser);
    }

    @Test
    @DisplayName("resolveProject passes the correct projectId to the repository")
    void testResolveProjectPassesCorrectProjectId() {
        UUID otherId = UUID.randomUUID();
        when(projectRepository.findByIdAndOwner(projectId, currentUser))
                .thenReturn(Optional.of(project));
        when(projectRepository.findByIdAndOwner(otherId, currentUser))
                .thenReturn(Optional.empty());

        assertThat(projectUtils.resolveProject(projectId)).isEqualTo(project);
        assertThat(projectUtils.resolveProject(otherId)).isNull();

        verify(projectRepository, times(1)).findByIdAndOwner(projectId, currentUser);
        verify(projectRepository, times(1)).findByIdAndOwner(otherId, currentUser);
    }

    @Test
    @DisplayName("resolveProject returns null when the project belongs to a different user")
    void testResolveProjectReturnsNullWhenProjectBelongsToDifferentUser() {
        when(projectRepository.findByIdAndOwner(projectId, currentUser))
                .thenReturn(Optional.empty());

        ProjectEntity result = projectUtils.resolveProject(projectId);

        assertThat(result).isNull();
        verify(projectRepository, times(1)).findByIdAndOwner(projectId, currentUser);
    }

    @Test
    @DisplayName("resolveProject returns null when project does not exist")
    void testResolveProjectReturnsNullWhenProjectNotFound() {
        when(projectRepository.findByIdAndOwner(any(), eq(currentUser)))
                .thenReturn(Optional.empty());

        ProjectEntity result = projectUtils.resolveProject(projectId);

        assertThat(result).isNull();
    }
}
