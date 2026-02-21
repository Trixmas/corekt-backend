package lab.corekt.backend.modules.mistral;/*
package lab.corekt.corektbackend.modules.mistral;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.UUID;

public class MistralService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MistralService(
        RestClient.Builder restClientBuilder,
        @Value("${mistral.api.token}") String token
    ) {
        this.restClient = restClientBuilder
            .baseUrl("https://api.mistral.ai/v1/conversations")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();

        this.objectMapper = new ObjectMapper();
    }

    public JsonNode sendMessage(String message) {
        Map<String, Object> body = Map.of(
            "model", "mistral-medium-latest",
            "inputs", new Object[]{
                Map.of(
                    "role", "user",
                    "content", message
                )
            }
        );

        String responseBody = restClient.post()
            .body(body)
            .retrieve()
            .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Mistral response", e);
        }
    }

    public JsonNode sendAdviceResponse(String message) throws Exception {
        Map<String, Object> body = Map.of(
            "model", "mistral-medium-latest",
            "inputs", new Object[]{
                Map.of(
                    "role", "user",
                    "content", message
                )
            }
        );

        long start = System.currentTimeMillis();
        ObjectNode result = objectMapper.createObjectNode();

        try {
            String responseBody = restClient.post()
                .body(body)
                .retrieve()
                .onStatus(
                    HttpStatusCode::isError,
                    (_, response) -> {
                        String errorBody = response.getBody().readAllBytes().length == 0
                            ? ""
                            : new String(response.getBody().readAllBytes());

                        throw new RuntimeException(
                            "HTTP " + response.getStatusCode().value() + " -> " + errorBody
                        );
                    }
                )
                .body(String.class);

            long duration = System.currentTimeMillis() - start;

            if (responseBody == null || responseBody.isBlank()) {
                result.put("isError", true);
                result.put("duration", duration);
                return result;
            }

            ObjectNode apiJson = objectMapper.readValue(responseBody, ObjectNode.class);

            result.put("responseId", UUID.randomUUID().toString());
            result.put("isError", false);
            result.put("duration", duration);
            result.put("content", apiJson.toString());

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;

            result.put("isError", true);
            result.put("duration", duration);
            result.put("content", e.getMessage());
            return result;
        }
    }
}
*/
