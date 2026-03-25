package lab.corekt.backend.modules.project.versions.service;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lab.corekt.backend.modules.project.parsing.repository.DataLinkRepository;
import lab.corekt.backend.modules.project.versions.dto.VersionDetailsResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock
    private VersionRepository versionRepository;

    @Mock
    private DataLinkRepository dataLinkRepository;

    @InjectMocks
    private VersionService versionService;

    private ProjectEntity project;

    @BeforeEach
    void setUp() {
        project = mock(ProjectEntity.class);
    }

    private VersionEntity buildVersion(String title, int versionNumber) {
        VersionEntity entity = new VersionEntity(project, title);
        entity.setVersionNumber(versionNumber);
        return entity;
    }

    private VersionRequest buildRequest(String title) {
        VersionRequest request = new VersionRequest();
        request.setTitle(title);
        return request;
    }

    // ─── getAllByProject ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all versions mapped to responses for a given project")
    void testGetAllByProjectReturnsMappedResponses() {
        VersionEntity v1 = buildVersion("Alpha", 1);
        VersionEntity v2 = buildVersion("Beta", 2);

        when(versionRepository.findByProject(project)).thenReturn(List.of(v1, v2));

        List<VersionResponse> result = versionService.getAllByProject(project);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Alpha");
        assertThat(result.get(1).getTitle()).isEqualTo("Beta");
        verify(versionRepository).findByProject(project);
    }

    @Test
    @DisplayName("Should return empty list when project has no versions")
    void testGetAllByProjectReturnsEmptyListWhenNoVersions() {
        when(versionRepository.findByProject(project)).thenReturn(List.of());

        List<VersionResponse> result = versionService.getAllByProject(project);

        assertThat(result).isEmpty();
        verify(versionRepository).findByProject(project);
    }

    // ─── getOneVersion ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return version response when version exists")
    void testGetVersionReturnsResponseWhenFound() {
        UUID versionId = UUID.randomUUID();
        VersionEntity entity = buildVersion("Release 1", 1);

        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(entity));
        when(dataLinkRepository.findByVersionId(entity.getId())).thenReturn(List.of());

        VersionDetailsResponse result = versionService.getVersion(project, versionId);

        assertThat(result.getTitle()).isEqualTo("Release 1");
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(VersionStatus.DRAFT);
        assertThat(result.getIssues()).isNotNull();
        assertThat(result.getDataLinks()).isNotNull();
        assertThat(result.getIssues()).isNotNull();
        verify(versionRepository).findByProjectAndId(project, versionId);
    }

    @Test
    @DisplayName("Should throw 404 when version is not found")
    void testGetVersionThrows404WhenNotFound() {
        UUID versionId = UUID.randomUUID();

        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> versionService.getVersion(project, versionId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Version not found");
        verify(versionRepository).findByProjectAndId(project, versionId);
    }

    // ─── createVersion ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create version with number 1 when no versions exist")
    void testCreateVersionAssignsVersionNumberOneWhenNoPreviousVersions() {
        VersionRequest request = buildRequest("First Release");
        VersionEntity saved = buildVersion("First Release", 1);

        when(versionRepository.findTopByProjectOrderByVersionNumberDesc(project)).thenReturn(Optional.empty());
        when(versionRepository.save(any(VersionEntity.class))).thenReturn(saved);

        VersionResponse result = versionService.createVersion(project, request);

        assertThat(result.getTitle()).isEqualTo("First Release");
        assertThat(result.getVersion()).isEqualTo(1);
        verify(versionRepository).findTopByProjectOrderByVersionNumberDesc(project);
        verify(versionRepository).save(any(VersionEntity.class));
    }

    @Test
    @DisplayName("Should create version with incremented number when versions already exist")
    void testCreateVersionIncrementsVersionNumberWhenPreviousVersionsExist() {
        VersionRequest request = buildRequest("Second Release");
        VersionEntity existing = buildVersion("First Release", 3);
        VersionEntity saved = buildVersion("Second Release", 4);

        when(versionRepository.findTopByProjectOrderByVersionNumberDesc(project)).thenReturn(Optional.of(existing));
        when(versionRepository.save(any(VersionEntity.class))).thenReturn(saved);

        VersionResponse result = versionService.createVersion(project, request);

        assertThat(result.getTitle()).isEqualTo("Second Release");
        assertThat(result.getVersion()).isEqualTo(4);
        verify(versionRepository).findTopByProjectOrderByVersionNumberDesc(project);
        verify(versionRepository).save(any(VersionEntity.class));
    }

    // ─── updateVersion ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update version title when title is provided")
    void testUpdateVersionUpdatesTitleWhenTitleIsProvided() {
        UUID versionId = UUID.randomUUID();
        VersionEntity entity = buildVersion("Old Title", 1);
        VersionRequest request = buildRequest("New Title");

        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(entity));
        when(versionRepository.save(entity)).thenReturn(entity);

        VersionResponse result = versionService.updateVersion(project, versionId, request);

        assertThat(result.getTitle()).isEqualTo("New Title");
        verify(versionRepository).findByProjectAndId(project, versionId);
        verify(versionRepository).save(entity);
    }

    @Test
    @DisplayName("Should not change title when request title is null")
    void testUpdateVersionKeepsTitleWhenRequestTitleIsNull() {
        UUID versionId = UUID.randomUUID();
        VersionEntity entity = buildVersion("Original Title", 1);
        VersionRequest request = new VersionRequest();
        // title is null

        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(entity));
        when(versionRepository.save(entity)).thenReturn(entity);

        VersionResponse result = versionService.updateVersion(project, versionId, request);

        assertThat(result.getTitle()).isEqualTo("Original Title");
        verify(versionRepository).save(entity);
    }

    @Test
    @DisplayName("Should throw 404 when updating a version that does not exist")
    void testUpdateVersionThrows404WhenNotFound() {
        UUID versionId = UUID.randomUUID();
        VersionRequest request = buildRequest("Title");

        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> versionService.updateVersion(project, versionId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Version not found");
        verify(versionRepository, never()).save(any());
    }

    //    ─── deleteVersion ────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should delete version when it exists")
    void testDeleteVersionDeletesWhenFound() {
        UUID versionId = UUID.randomUUID();
        VersionEntity entity = buildVersion("To Be Deleted", 1);

        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(entity));
        versionService.deleteVersion(project, versionId);
        verify(versionRepository).delete(entity);
    }

    @Test
    @DisplayName("Should not delete version when it does not exist")
    void testDeleteVersionDoesNotDeleteAndThrowsWhenNotFound() {
        UUID versionId = UUID.randomUUID();
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> versionService.deleteVersion(project, versionId));
        verify(versionRepository, never()).delete(any());
    }

    // ─── toResponse (via public methods) ──────────────────────────────────────

    @Test
    @DisplayName("Should map all version fields correctly in response")
    void testToResponseMapsAllFieldsCorrectly() {
        UUID versionId = UUID.randomUUID();
        VersionEntity entity = buildVersion("v1.0", 5);
        entity.setStatus(VersionStatus.IN_PROGRESS);

        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(entity));
        when(dataLinkRepository.findByVersionId(entity.getId())).thenReturn(List.of());

        VersionResponse response = versionService.getVersion(project, versionId);

        assertThat(response.getTitle()).isEqualTo("v1.0");
        assertThat(response.getVersion()).isEqualTo(5);
        assertThat(response.getStatus()).isEqualTo(VersionStatus.IN_PROGRESS);
        assertThat(response.getIssues().getMinor()).isEqualTo(0);
        assertThat(response.getIssues().getMajor()).isEqualTo(0);
        assertThat(response.getIssues().getCritical()).isEqualTo(0);
        assertThat(response.getIssues().getResolved()).isEqualTo(0);
    }
}


