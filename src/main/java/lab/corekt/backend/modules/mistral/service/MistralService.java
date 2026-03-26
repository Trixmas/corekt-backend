package lab.corekt.backend.modules.mistral.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lab.corekt.backend.modules.mistral.dto.DetectedUserStory;
import lab.corekt.backend.modules.mistral.dto.InterviewAnalysisResult;
import lab.corekt.backend.modules.mistral.dto.QqoqcpDetails;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.report.entity.AIAnalysisEntity;
import lab.corekt.backend.modules.project.report.repository.AIAnalysisRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.*;

@Service
public class MistralService {

    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String agentId;
    private final Integer agentVersion;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final ParsedDataRepository parsedDataRepository;
    private final InterviewPdfTextExtractor interviewPdfTextExtractor;

    public MistralService(
        EmbeddingModel embeddingModel,
        ObjectMapper objectMapper,
        AIAnalysisRepository aiAnalysisRepository,
        ParsedDataRepository parsedDataRepository,
        RestClient.Builder restClientBuilder,
        @Value("${mistral.api.token}") String mistralApiToken,
        @Value("${mistral.agent.id}") String agentId,
        @Value("${mistral.agent.version}") Integer agentVersion,
        @Value("${mistral.api.base-url:https://api.mistral.ai}")
        String mistralBaseUrl,
        InterviewPdfTextExtractor interviewPdfTextExtractor
    ) {
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.parsedDataRepository = parsedDataRepository;
        this.agentId = agentId;
        this.agentVersion = agentVersion;
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(120));

        this.restClient = restClientBuilder
            .baseUrl(mistralBaseUrl)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Authorization", "Bearer " + mistralApiToken)
            .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
            .build();
        this.interviewPdfTextExtractor = interviewPdfTextExtractor;
    }

    public List<EmbeddingTuple> embedParsedData(List<ParsedDataEntity> parsedData) {
        List<EmbeddingTuple> embeddings = new ArrayList<>();
        List<float[]> embeddingResponse = embeddingModel.embed(parsedData.stream().map(ParsedDataEntity::getContent).toList());
        embeddingResponse.forEach(embedding -> {
            int index = embeddingResponse.indexOf(embedding);
            embeddings.add(new EmbeddingTuple(parsedData.get(index), embedding));
        });
        return embeddings;
    }

    public InterviewAnalysisResult analyzeInterview(String interviewText) {
        if (interviewText == null || interviewText.isBlank()) {
            throw new IllegalArgumentException("Interview text must not be blank");
        }

        String output = null;
        try {
            output = requestInterviewAnalysis(interviewText);
            return objectMapper.readValue(output, InterviewAnalysisResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to analyze interview with Mistral: invalid JSON response. " +
                "Output tail: " + summarizeOutputTail(output), e);
        } catch (Exception e) {
            System.err.println("Failed to analyze interview with Mistral: " + e.getMessage());
            throw new IllegalStateException("Failed to analyze interview with Mistral", e);
        }
    }

    private String requestInterviewAnalysis(String interviewText) throws JsonProcessingException {
        Map<String, Object> requestBody = Map.of(
            "agent_id", agentId,
            "agent_version", agentVersion,
            "inputs", List.of(
                Map.of(
                    "role", "user",
                    "content", interviewText)));

        String responseBody = restClient.post()
            .uri("/v1/conversations")
            .body(requestBody)
            .retrieve()
            .body(String.class);

        JsonNode root = objectMapper.readTree(responseBody);
        return extractAssistantOutput(root);
    }

    private String summarizeOutputTail(String output) {
        if (output == null) {
            return "<empty output>";
        }

        int tailLength = 180;
        String compact = output.replaceAll("\\s+", " ").trim();
        if (compact.length() <= tailLength) {
            return compact;
        }

        return "..." + compact.substring(compact.length() - tailLength);
    }

    public AIAnalysisEntity saveInterviewAnalysis(FileEntity interviewFile, InterviewAnalysisResult analysisResult) {
        if (interviewFile == null) {
            throw new IllegalArgumentException("Interview file must not be null");
        }

        if (analysisResult == null) {
            throw new IllegalArgumentException("Interview analysis result must not be null");
        }

        try {
            AIAnalysisEntity entity = new AIAnalysisEntity(interviewFile);
            entity.setQualityScore(analysisResult.getQualityScore());
            entity.setSummary(analysisResult.getSummary());
            entity.setIssues(objectMapper.writeValueAsString(analysisResult.getDetectedIssues()));
            entity.setSuggestions(objectMapper.writeValueAsString(analysisResult.getSuggestions()));
            return aiAnalysisRepository.save(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save interview analysis", e);
        }
    }

    public AIAnalysisEntity analyzeAndSaveInterview(FileEntity interviewFile, String interviewText) {
        InterviewAnalysisResult analysisResult = analyzeInterview(interviewText);
        AIAnalysisEntity savedAnalysis = saveInterviewAnalysis(interviewFile, analysisResult);

        saveInterviewParsedData(interviewFile, analysisResult);
        return savedAnalysis;
    }

    @Async
    @Transactional
    public void processInterview(FileEntity file) {
        String interviewText = interviewPdfTextExtractor.extractText(file);
        analyzeAndSaveInterview(file, interviewText);
    }

    public List<ParsedDataEntity> saveInterviewParsedData(
        FileEntity interviewFile,
        InterviewAnalysisResult analysisResult
    ) {
        if (interviewFile == null) {
            throw new IllegalArgumentException("Interview file must not be null");
        }

        if (analysisResult == null) {
            throw new IllegalArgumentException("Interview analysis result must not be null");
        }

        List<ParsedDataEntity> batch = new ArrayList<>();
        QqoqcpDetails qqoqcpAnalysis = analysisResult.getQqoqcpAnalysis();
        if (qqoqcpAnalysis != null) {
            addParsedDataBatch(batch, interviewFile, DataType.ACTOR,
                qqoqcpAnalysis.getQui() == null ? null : qqoqcpAnalysis.getQui().getElementsFound());
            addParsedDataBatch(batch, interviewFile, DataType.ACTION,
                qqoqcpAnalysis.getQuoi() == null ? null : qqoqcpAnalysis.getQuoi().getElementsFound());
        }

        List<DetectedUserStory> detectedUserStories = analysisResult.getDetectedUserStories();
        if (detectedUserStories != null) {
            detectedUserStories.stream()
                .map(this::formatUserStory)
                .filter(Objects::nonNull)
                .map(content -> buildParsedData(interviewFile, DataType.US, content))
                .forEach(batch::add);
        }

        if (batch.isEmpty()) {
            return List.of();
        }

        return parsedDataRepository.saveAll(batch);
    }

    private String extractText(JsonNode contentChunks) {
        StringBuilder text = new StringBuilder();

        for (JsonNode chunk : contentChunks) {
            if (chunk.has("text")) {
                text.append(chunk.get("text").asText());
            }
        }

        String result = text.toString().trim();
        if (result.isEmpty()) {
            throw new IllegalStateException("Mistral message.output content does not contain text");
        }

        return result;
    }

    private String extractAssistantOutput(JsonNode root) {
        JsonNode outputs = root.get("outputs");
        if (outputs == null || !outputs.isArray()) {
            throw new IllegalStateException("Mistral conversation response does not contain outputs");
        }

        JsonNode lastMessageOutput = null;
        for (JsonNode output : outputs) {
            if ("message.output".equals(output.path("type").asText())) {
                lastMessageOutput = output;
            }
        }

        if (lastMessageOutput == null) {
            throw new IllegalStateException("Mistral conversation response does not contain a message.output entry");
        }

        JsonNode content = lastMessageOutput.get("content");
        if (content == null) {
            throw new IllegalStateException("Mistral message.output does not contain content");
        }

        if (content.isTextual()) {
            return content.asText();
        }

        if (content.isArray()) {
            return extractText(content);
        }

        throw new IllegalStateException("Mistral message.output content is expected to be either a string or an array");
    }

    private ParsedDataEntity buildParsedData(FileEntity interviewFile, DataType dataType, String content) {
        ParsedDataEntity parsedData = new ParsedDataEntity(dataType, content);
        parsedData.setFile(interviewFile);
        return parsedData;
    }

    private void addParsedDataBatch(List<ParsedDataEntity> batch, FileEntity interviewFile, DataType dataType,
                                    List<String> contents) {
        if (contents == null) {
            return;
        }

        contents.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(content -> !content.isBlank())
            .distinct()
            .forEach(content -> batch.add(buildParsedData(interviewFile, dataType, content)));
    }

    private String formatUserStory(DetectedUserStory userStory) {
        if (userStory == null) {
            return null;
        }

        String actor = trimToNull(userStory.getActor());
        String action = trimToNull(userStory.getAction());
        String benefit = trimToNull(userStory.getBenefit());

        if (actor == null || action == null || benefit == null) {
            return null;
        }

        return String.format("En tant que %s, je veux %s, afin de %s", actor, action, benefit);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    public record EmbeddingTuple(ParsedDataEntity parsedData, float[] embedding) {
        @Override
        public String toString() {
            return parsedData.getContent();
        }

        @Override
        public int hashCode() {
            return Objects.hash(parsedData, Arrays.hashCode(embedding));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            EmbeddingTuple other = (EmbeddingTuple) obj;
            return Objects.equals(parsedData, other.parsedData) && Arrays.equals(embedding, other.embedding);
        }
    }
}
