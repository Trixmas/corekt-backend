package lab.corekt.backend.modules.mistral.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import lab.corekt.backend.modules.mistral.dto.DetectedIssues;
import lab.corekt.backend.modules.mistral.dto.DetectedUserStory;
import lab.corekt.backend.modules.mistral.dto.InterviewAnalysisResult;
import lab.corekt.backend.modules.mistral.dto.InterviewIssue;
import lab.corekt.backend.modules.mistral.dto.InterviewSuggestion;
import lab.corekt.backend.modules.mistral.dto.QqoqcpDetails;
import lab.corekt.backend.modules.mistral.dto.QqoqcpDimensionDetailed;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.report.entity.AIAnalysisEntity;
import lab.corekt.backend.modules.project.report.repository.AIAnalysisRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;

class MistralServiceTest {

    private static final Properties TEST_PROPERTIES = loadTestProperties();
    private static final String MISTRAL_API_TOKEN = resolveTestProperty("mistral.api.token");
    private static final String MISTRAL_AGENT_ID = resolveTestProperty("mistral.agent.id");
    private static final int MISTRAL_AGENT_VERSION = Integer.parseInt(resolveTestProperty("mistral.agent.version"));
    private static final String MISTRAL_BASE_URL = "https://api.mistral.ai";

    private static final String SAMPLE_INTERVIEW = """
            Je suis responsable RH.
            Quand un employe pose une demande de conges, il remplit un formulaire sur l'intranet.
            Le manager doit valider la demande sous 48 heures.
            Ensuite, le service RH enregistre l'absence dans le SIRH pour eviter les conflits de planning.
            Pour les conges exceptionnels, ca depend du contexte et on traite parfois au cas par cas.
            """;

    private EmbeddingModel embeddingModel;
    private AIAnalysisRepository aiAnalysisRepository;
    private ParsedDataRepository parsedDataRepository;
    private ObjectMapper objectMapper;
    private MistralService mistralService;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        aiAnalysisRepository = mock(AIAnalysisRepository.class);
        parsedDataRepository = mock(ParsedDataRepository.class);
        objectMapper = new ObjectMapper();
        mistralService = new MistralService(
                embeddingModel,
                objectMapper,
                aiAnalysisRepository,
                parsedDataRepository,
                RestClient.builder(),
                MISTRAL_API_TOKEN,
                MISTRAL_AGENT_ID,
                MISTRAL_AGENT_VERSION,
                MISTRAL_BASE_URL,
                mock(InterviewPdfTextExtractor.class));
    }

    @Test
    @DisplayName("Should analyze a sample interview through the configured Mistral agent")
    @EnabledIfSystemProperty(named = "runMistralIntegrationTest", matches = "true")
    void testAnalyzeInterview() {
        when(aiAnalysisRepository.save(any(AIAnalysisEntity.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        FileEntity interviewFile = new FileEntity(FileType.INTERVIEW, "interview.pdf");
        interviewFile.setId(UUID.randomUUID());

        InterviewAnalysisResult result = mistralService.analyzeInterview(SAMPLE_INTERVIEW);

        AIAnalysisEntity saved = mistralService.saveInterviewAnalysis(interviewFile, result);

        assertThat(result).isNotNull();
        assertThat(result.getQualityScore()).isBetween(0, 100);
        assertThat(result.getSummary()).isNotBlank();
        assertThat(result.getQqoqcpAnalysis()).isNotNull();
        assertThat(saved).isNotNull();
        verify(aiAnalysisRepository).save(any(AIAnalysisEntity.class));
    }

    @Test
    @DisplayName("Should analyze and save a sample interview through the configured Mistral agent")
    @EnabledIfSystemProperty(named = "runMistralIntegrationTest", matches = "true")
    void testAnalyzeAndSaveInterview() {
        when(aiAnalysisRepository.save(any(AIAnalysisEntity.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(parsedDataRepository.saveAll(any()))
            .thenAnswer(inv -> inv.getArgument(0));

        FileEntity interviewFile = new FileEntity(FileType.INTERVIEW, "interview.pdf");
        interviewFile.setId(UUID.randomUUID());

        AIAnalysisEntity saved = mistralService.analyzeAndSaveInterview(interviewFile, SAMPLE_INTERVIEW);

        assertThat(saved).isNotNull();
        assertThat(saved.getFile()).isEqualTo(interviewFile);
        assertThat(saved.getQualityScore()).isBetween(0, 100);
        assertThat(saved.getSummary()).isNotBlank();
        verify(aiAnalysisRepository).save(any(AIAnalysisEntity.class));
        verify(parsedDataRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should save interview analysis fields in AIAnalysisEntity")
    void testSaveInterviewAnalysis() throws Exception {
        FileEntity interviewFile = new FileEntity(FileType.INTERVIEW, "interview.pdf");

        InterviewIssue issue = new InterviewIssue();
        issue.setId("EVA-001");
        issue.setExcerpt("Ca depend du contexte");
        issue.setDescription("Reponse evasive");
        issue.setSeverity("high");
        issue.setImpact("BPMN");

        DetectedIssues detectedIssues = new DetectedIssues();
        detectedIssues.setEvasiveAnswers(List.of(issue));
        detectedIssues.setContradictions(List.of());
        detectedIssues.setMissingCriticalInfo(List.of());
        detectedIssues.setAmbiguities(List.of());
        detectedIssues.setRedFlags(List.of());

        InterviewSuggestion suggestion = new InterviewSuggestion();
        suggestion.setId("SUG-001");
        suggestion.setCategory("ambiguity");
        suggestion.setTitle("Clarifier le contexte");
        suggestion.setQuestion("Pouvez-vous preciser dans quels cas cela depend du contexte ?");
        suggestion.setPriority("high");
        suggestion.setRationale("Necessaire pour lever l'ambiguite");
        suggestion.setRelatedIssues(List.of("EVA-001"));

        InterviewAnalysisResult analysisResult = new InterviewAnalysisResult();
        analysisResult.setQualityScore(65);
        analysisResult.setSummary("Resume de test");
        analysisResult.setDetectedIssues(detectedIssues);
        analysisResult.setSuggestions(List.of(suggestion));

        when(aiAnalysisRepository.save(any(AIAnalysisEntity.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        AIAnalysisEntity saved = mistralService.saveInterviewAnalysis(interviewFile, analysisResult);

        assertThat(saved).isNotNull();
        assertThat(saved.getFile()).isEqualTo(interviewFile);
        assertThat(saved.getQualityScore()).isEqualTo(65);
        assertThat(saved.getSummary()).isEqualTo("Resume de test");
        assertThat(saved.getIssues()).isEqualTo(objectMapper.writeValueAsString(detectedIssues));
        assertThat(saved.getSuggestions()).isEqualTo(objectMapper.writeValueAsString(List.of(suggestion)));
        verify(aiAnalysisRepository).save(any(AIAnalysisEntity.class));
    }

    @Test
    @DisplayName("Should save ACTOR ACTION and US parsed data from interview analysis")
    void testSaveInterviewParsedData() {
        FileEntity interviewFile = new FileEntity(FileType.INTERVIEW, "interview.pdf");

        QqoqcpDimensionDetailed qui = new QqoqcpDimensionDetailed();
        qui.setElementsFound(List.of("employe", "manager"));

        QqoqcpDimensionDetailed quoi = new QqoqcpDimensionDetailed();
        quoi.setElementsFound(List.of("remplir un formulaire", "valider la demande"));

        QqoqcpDetails qqoqcpAnalysis = new QqoqcpDetails();
        qqoqcpAnalysis.setQui(qui);
        qqoqcpAnalysis.setQuoi(quoi);

        DetectedUserStory userStory = new DetectedUserStory();
        userStory.setActor("manager");
        userStory.setAction("valider une demande de conges");
        userStory.setBenefit("approuver les absences");

        InterviewAnalysisResult analysisResult = new InterviewAnalysisResult();
        analysisResult.setQqoqcpAnalysis(qqoqcpAnalysis);
        analysisResult.setDetectedUserStories(List.of(userStory));

        when(parsedDataRepository.saveAll(any()))
            .thenAnswer(inv -> inv.getArgument(0));

        List<ParsedDataEntity> saved = mistralService.saveInterviewParsedData(interviewFile, analysisResult);

        assertThat(saved).hasSize(5); // 2 ACTORS + 2 ACTIONS + 1 US = 5 :)
        assertThat(saved)
            .extracting(ParsedDataEntity::getDataType, ParsedDataEntity::getContent)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(DataType.ACTOR, "employe"),
                org.assertj.core.groups.Tuple.tuple(DataType.ACTOR, "manager"),
                org.assertj.core.groups.Tuple.tuple(DataType.ACTION, "remplir un formulaire"),
                org.assertj.core.groups.Tuple.tuple(DataType.ACTION, "valider la demande"),
                org.assertj.core.groups.Tuple.tuple(
                    DataType.US,
                    "En tant que manager, je veux valider une demande de conges, afin de approuver les absences"
                )
            );
        assertThat(saved).allMatch(parsedData -> parsedData.getFile() == interviewFile);
        verify(parsedDataRepository).saveAll(any());
    }

    private static Properties loadTestProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = MistralServiceTest.class.getResourceAsStream("/application-test.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("application-test.properties not found");
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load application-test.properties", e);
        }
    }

    private static String resolveTestProperty(String key) {
        String value = TEST_PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing test property: " + key);
        }

        if (!value.startsWith("${") || !value.endsWith("}")) {
            return value;
        }

        String placeholder = value.substring(2, value.length() - 1);
        String[] parts = placeholder.split(":", 2);
        String envName = parts[0];
        String defaultValue = parts.length > 1 ? parts[1] : "";

        String envValue = System.getenv(envName);
        return envValue == null || envValue.isBlank() ? defaultValue : envValue;
    }
}
