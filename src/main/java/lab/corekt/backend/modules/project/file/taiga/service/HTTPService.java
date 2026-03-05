package lab.corekt.backend.modules.project.file.taiga.service;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Service class for making HTTP requests to the Taiga API using Spring's RestClient
 */
@Service
public class HTTPService {

    private static final String TAIGA_URL = "https://api.taiga.io/api/v1";

    private final RestClient restClient;

    public HTTPService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(TAIGA_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Make a GET request to the given path.
     *
     * @param path      the API path (e.g. "/projects")
     * @param authToken Bearer authentication token
     * @param headers   optional additional headers
     * @return the response body as a string
     */
    public String get(String path, String authToken, @Nullable Map<String, String> headers) {
        var spec = restClient.get().uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);

        if (headers != null) {
            headers.forEach(spec::header);
        }

        return spec.retrieve().body(String.class);
    }

    /**
     * Make a POST request to the given path.
     *
     * @param path the API path (e.g. "/auth")
     * @param body the JSON body as a string
     * @return the response body as a string
     */
    public String post(String path, String body) {
        return restClient.post().uri(path)
                .body(body)
                .retrieve()
                .body(String.class);
    }
}
