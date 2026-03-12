package lab.corekt.backend.modules.upload.taiga.service;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.service.FileService;
import lab.corekt.backend.modules.upload.taiga.dto.TaigaCredentials;
import lab.corekt.backend.modules.upload.taiga.dto.US;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaigaServiceTest {

    @Mock
    private HTTPService httpService;

    @Mock
    private FileService fileService;

    @Mock
    private ParsedDataRepository parsedDataRepository;

    @InjectMocks
    private TaigaService taigaService;

    private VersionEntity version;
    private FileEntity file;

    @BeforeEach
    void setUp() {
        version = new VersionEntity();
        file = new FileEntity();
    }

    @Test
    @DisplayName("authenticate returns token and user id when Taiga response is valid")
    void testAuthenticateReturnsCredentialsWhenResponseIsValid() throws Exception {
        when(httpService.post(eq("/auth"), anyString()))
                .thenReturn("""
                        {
                          "auth_token": "token-123",
                          "id": 42
                        }
                    """);

        TaigaCredentials result = taigaService.authenticate("alice", "secret");

        assertThat(result.token()).isEqualTo("token-123");
        assertThat(result.userId()).isEqualTo("42");
    }

    @Test
    @DisplayName("authenticate throws when Taiga response is blank")
    void testAuthenticateThrowsWhenResponseIsBlank() {
        when(httpService.post(eq("/auth"), anyString())).thenReturn(" ");

        assertThatThrownBy(() -> taigaService.authenticate("alice", "secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid response from Taiga");
    }

    @Test
    @DisplayName("authenticate throws when token or id is missing")
    void testAuthenticateThrowsWhenTokenOrIdIsMissing() {
        when(httpService.post(eq("/auth"), anyString()))
            .thenReturn("""
                    {
                      "username": "alice"
                    }
                """);

        assertThatThrownBy(() -> taigaService.authenticate("alice", "secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid credentials or missing token");
    }

    @Test
    @DisplayName("fetchProjects returns ordered map of project names to ids")
    void testFetchProjectsReturnsProjectMap() throws Exception {
        when(httpService.get("/projects?member=42", "token", null))
                .thenReturn("""
                        [
                          { "id": 10, "name": "Project A" },
                          { "id": 20, "name": "Project B" }
                        ]
                    """);

        var result = taigaService.fetchProjects("token", "42");

        assertThat(result).containsEntry("Project A", "10");
        assertThat(result).containsEntry("Project B", "20");
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("fetchAndSaveUserStories returns empty list when Taiga returns no user stories")
    void testFetchAndSaveUserStoriesReturnsEmptyListWhenNoStories() throws Exception {
        when(fileService.getOrCreate(FileType.US, "User Stories", version)).thenReturn(file);
        when(httpService.get(eq("/userstories?project=12"), eq("token"), anyMap()))
                .thenReturn("[]");

        List<US> result = taigaService.fetchAndSaveUserStories("12", "token", version);

        assertThat(result).isEmpty();
        verify(parsedDataRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("fetchAndSaveUserStories saves parsed data and returns stories")
    void testFetchAndSaveUserStoriesSavesParsedDataAndReturnsStories() throws Exception {
        when(fileService.getOrCreate(FileType.US, "User Stories", version)).thenReturn(file);

        when(httpService.get(eq("/userstories?project=12"), eq("token"), anyMap()))
                .thenReturn("""
                        [
                          { "id": 100, "subject": "US 1" },
                          { "id": 101, "subject": "US 2" }
                        ]
                    """);

        when(httpService.get("/userstories/100", "token", null))
                .thenReturn("""
                        { "description": "Desc 1" }
                    """);

        when(httpService.get("/userstories/101", "token", null))
                .thenReturn("""
                        { "description": "Desc 2" }
                    """);

        List<US> result = taigaService.fetchAndSaveUserStories("12", "token", version);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("US 1");
        assertThat(result.get(0).getDescription()).isEqualTo("Desc 1");
        assertThat(result.get(1).getTitle()).isEqualTo("US 2");
        assertThat(result.get(1).getDescription()).isEqualTo("Desc 2");

        ArgumentCaptor<List<ParsedDataEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(parsedDataRepository).saveAll(captor.capture());

        List<ParsedDataEntity> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getFile()).isEqualTo(file);
        assertThat(saved.get(1).getFile()).isEqualTo(file);
    }

    @Test
    @DisplayName("fetchAndSaveUserStories keeps empty description when story detail call fails")
    void testFetchAndSaveUserStoriesUsesEmptyDescriptionWhenStoryDetailFails() throws Exception {
        when(fileService.getOrCreate(FileType.US, "User Stories", version)).thenReturn(file);

        when(httpService.get(eq("/userstories?project=12"), eq("token"), anyMap()))
                .thenReturn("""
                        [
                          { "id": 100, "subject": "US 1" }
                        ]
                    """);

        when(httpService.get("/userstories/100", "token", null)).thenThrow(new RuntimeException("Detail error"));

        List<US>  result = taigaService.fetchAndSaveUserStories("12", "token", version);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("US 1");
        assertThat(result.getFirst().getDescription()).isEqualTo("");

        verify(parsedDataRepository).saveAll(anyList());
    }
}