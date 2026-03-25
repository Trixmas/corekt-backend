package lab.corekt.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import lab.corekt.backend.modules.mistral.service.MistralService;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.repository.DataLinkRepository;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.parsing.service.DataLinkService;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
class DataLinkServiceIntegrationTest {

    @Autowired
    private DataLinkService dataLinkService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ParsedDataRepository parsedDataRepository;

    @Autowired
    private DataLinkRepository dataLinkRepository;

    @Test
    @DisplayName("Should load parsed data and get real embeddings by file type")
    @EnabledIfSystemProperty(named = "runMistralEmbeddingIntegrationTest", matches = "true")
    void testGetEmbeddingsByFileType() {
        VersionEntity version = createVersionWithParsedData();
        Map<FileType, List<MistralService.EmbeddingTuple>> embeddingsByFileType = dataLinkService.getEmbeddingsByFileType(version);

        assertThat(embeddingsByFileType).hasSize(FileType.values().length);
        assertThat(embeddingsByFileType.get(FileType.INTERVIEW)).hasSize(1);
        assertThat(embeddingsByFileType.get(FileType.BPMN)).hasSize(1);
        assertThat(embeddingsByFileType.get(FileType.US)).hasSize(1);
        assertThat(embeddingsByFileType.get(FileType.MCD)).hasSize(1);

        assertThat(embeddingsByFileType.get(FileType.INTERVIEW).getFirst().parsedData().getContent()).isEqualTo("manager");
        assertThat(embeddingsByFileType.get(FileType.BPMN).getFirst().parsedData().getContent()).isEqualTo("manager");
        assertThat(embeddingsByFileType.get(FileType.US).getFirst().parsedData().getContent())
            .contains("En tant que manager");
        assertThat(embeddingsByFileType.get(FileType.MCD).getFirst().parsedData().getContent()).isEqualTo("absence");

        for (List<MistralService.EmbeddingTuple> embeddings : embeddingsByFileType.values()) {
            for (MistralService.EmbeddingTuple embedding : embeddings) {
                assertThat(embedding.embedding().length).isGreaterThan(0);
            }
        }
    }

    @Test
    @DisplayName("Should save real data links in database")
    @EnabledIfSystemProperty(named = "runMistralEmbeddingIntegrationTest", matches = "true")
    void testSaveDataLinks() {
        VersionEntity version = createVersionWithParsedData();

        List<DataLinkEntity> savedLinks = dataLinkService.saveDataLinks(version);

        assertThat(savedLinks).hasSize(4);
        assertThat(savedLinks).allMatch(link -> link.getId() != null);

        long exactLinks = savedLinks.stream()
            .filter(link -> link.getMatch() == MatchType.EXACT)
            .count();

        long missingLinks = savedLinks.stream()
            .filter(link -> link.getMatch() == MatchType.MISSING)
            .count();

        assertThat(exactLinks).isEqualTo(2);
        assertThat(missingLinks).isEqualTo(2);

        List<DataLinkEntity> persistedLinks = dataLinkRepository.findAll();

        assertThat(persistedLinks).hasSize(4);
        assertThat(persistedLinks)
            .filteredOn(link -> link.getMatch() == MatchType.EXACT)
            .allMatch(link -> "manager".equals(link.getSourceParsedData().getContent()))
            .allMatch(link -> "manager".equals(link.getTargetParsedData().getContent()));
    }

    private VersionEntity createVersionWithParsedData() {
        UserEntity user = userRepository.save(new UserEntity(randomEmail(), "salim"));
        ProjectEntity project = projectRepository.save(new ProjectEntity("Corekt", "Test data linking", user));
        VersionEntity version = versionRepository.findTopByProjectOrderByVersionNumberDesc(project).orElseThrow();

        FileEntity interviewFile = saveFile(version, FileType.INTERVIEW, "interview.pdf");
        FileEntity bpmnFile = saveFile(version, FileType.BPMN, "process.bpmn");
        FileEntity usFile = saveFile(version, FileType.US, "user-stories");
        FileEntity mcdFile = saveFile(version, FileType.MCD, "mcd");

        parsedDataRepository.saveAll(List.of(
            parsedData(interviewFile, DataType.ACTOR, "manager"),
            parsedData(bpmnFile, DataType.ACTOR, "manager"),
            parsedData(usFile, DataType.US, "En tant que manager, je veux valider une demande, afin de gerer les absences"),
            parsedData(mcdFile, DataType.ENTITY, "absence")
        ));

        return version;
    }

    private FileEntity saveFile(VersionEntity version, FileType fileType, String fileName) {
        FileEntity file = new FileEntity(fileType, fileName);
        file.setVersion(version);
        return fileRepository.save(file);
    }

    private ParsedDataEntity parsedData(FileEntity file, DataType dataType, String content) {
        ParsedDataEntity parsedData = new ParsedDataEntity(dataType, content);
        parsedData.setFile(file);
        return parsedData;
    }

    private String randomEmail() {
        return "salim+" + UUID.randomUUID() + "@corekt.test";
    }
}
