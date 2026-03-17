package lab.corekt.backend.modules.taiga.service;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.taiga.dto.TaigaCredentials;
import lab.corekt.backend.modules.taiga.dto.UserStory;
import lab.corekt.backend.modules.taiga.exception.TaigaAuthException;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class TaigaServiceTest {

    @Mock
    private FileService fileService;

    @Mock
    private ParsedDataRepository parsedDataRepository;

    private MockRestServiceServer server;
    private TaigaService taigaService;
    private VersionEntity version;
    private FileEntity file;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        taigaService = new TaigaService(builder, "https://api.taiga.io/api/v1", fileService, parsedDataRepository);
        version = new VersionEntity("Version 1");
        file = new FileEntity(FileType.US, "User Stories");
    }

    @Test
    @DisplayName("authenticate returns token and user id when Taiga response is valid")
    void testAuthenticateReturnsCredentialsWhenResponseIsValid() {
        server.expect(requestTo("https://api.taiga.io/api/v1/auth"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {"username":"alice","password":"secret","type":"normal"}
                """))
            .andRespond(withSuccess("""
                {"auth_token":"token-123","id":42}
                """, MediaType.APPLICATION_JSON));

        TaigaCredentials result = taigaService.authenticate("alice", "secret");

        assertThat(result.token()).isEqualTo("token-123");
        assertThat(result.userId()).isEqualTo("42");
        server.verify();
    }

    @Test
    @DisplayName("authenticate returns 401 error when Taiga credentials are invalid")
    void testAuthenticateThrowsAuthExceptionWhenCredentialsAreInvalid() {
        server.expect(requestTo("https://api.taiga.io/api/v1/auth"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> taigaService.authenticate("alice", "wrong-password"))
            .isInstanceOf(TaigaAuthException.class)
            .hasMessage("Invalid Taiga credentials");
    }

    @Test
    @DisplayName("fetchProjects returns ordered map of project names to ids")
    void testFetchProjectsReturnsOrderedMap() {
        server.expect(requestTo("https://api.taiga.io/api/v1/projects?member=42"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
            .andRespond(withSuccess("""
                [
                  {"id":10,"name":"Project A"},
                  {"id":20,"name":"Project B"}
                ]
                """, MediaType.APPLICATION_JSON));

        var result = taigaService.fetchProjects("token-123", "42");

        assertThat(result).containsExactlyEntriesOf(new java.util.LinkedHashMap<>() {{
            put("Project A", "10");
            put("Project B", "20");
        }});
        server.verify();
    }

    @Test
    @DisplayName("fetchProjects returns 401 error when Taiga token is invalid")
    void testFetchProjectsThrowsAuthExceptionWhenTokenIsInvalid() {
        server.expect(requestTo("https://api.taiga.io/api/v1/projects?member=42"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> taigaService.fetchProjects("token-123", "42"))
            .isInstanceOf(TaigaAuthException.class)
            .hasMessage("Taiga token is invalid or expired");
    }

    @Test
    @DisplayName("fetchAndSaveUserStories returns empty list when Taiga returns no stories")
    void testFetchAndSaveUserStoriesReturnsEmptyListWhenNoStories() {
        server.expect(requestTo("https://api.taiga.io/api/v1/userstories?project=12"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
            .andExpect(header("x-disable-pagination", "True"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<UserStory> result = taigaService.fetchAndSaveUserStories("12", "token-123", version);

        assertThat(result).isEmpty();
        verify(fileService, never()).getOrCreate(FileType.US, "User Stories", version);
        verify(parsedDataRepository, never()).saveAll(anyList());
        server.verify();
    }

    @Test
    @DisplayName("fetchAndSaveUserStories persists parsed data and keeps empty descriptions on detail failure")
    void testFetchAndSaveUserStoriesPersistsParsedData() {
        when(fileService.getOrCreate(FileType.US, "User Stories", version)).thenReturn(file);

        server.expect(requestTo("https://api.taiga.io/api/v1/userstories?project=12"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
            .andExpect(header("x-disable-pagination", "True"))
            .andRespond(withSuccess("""
                [
                  {"id":100,"subject":"US 1"},
                  {"id":101,"subject":"US 2"}
                ]
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://api.taiga.io/api/v1/userstories/100"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
            .andRespond(withSuccess("""
                {"description":"Desc 1"}
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://api.taiga.io/api/v1/userstories/101"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
            .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        List<UserStory> result = taigaService.fetchAndSaveUserStories("12", "token-123", version);

        assertThat(result).containsExactly(
            new UserStory("US 1", "Desc 1"),
            new UserStory("US 2", "")
        );

        ArgumentCaptor<List<ParsedDataEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(parsedDataRepository).saveAll(captor.capture());

        List<ParsedDataEntity> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getFile()).isEqualTo(file);
        assertThat(saved.get(0).getContent()).contains("US 1").contains("Desc 1");
        assertThat(saved.get(1).getFile()).isEqualTo(file);
        assertThat(saved.get(1).getContent()).isEqualTo("US: {title = US 2}");
        server.verify();
    }

    @Test
    @DisplayName("fetchAndSaveUserStories returns 401 error when Taiga token is invalid")
    void testFetchAndSaveUserStoriesThrowsAuthExceptionWhenTokenIsInvalid() {
        server.expect(requestTo("https://api.taiga.io/api/v1/userstories?project=12"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> taigaService.fetchAndSaveUserStories("12", "token-123", version))
            .isInstanceOf(TaigaAuthException.class)
            .hasMessage("Taiga token is invalid or expired");
    }
}
