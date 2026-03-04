package lab.corekt.backend.modules.project.file.taiga.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service class for making HTTP requests
 */
@Service
public class HTTPService {
    private static final HttpClient client = HttpClient.newHttpClient();

    private HTTPService() {
    }

    /**
     * Make an HTTP request to the given URL with the specified method and body
     *
     * @param url    the URL to make the request to
     * @param method the HTTP method to use (e.g., "GET", "POST", etc.)
     * @param body   the body of the request (for POST, PUT, etc.)
     * @return the response body as a string
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public static String makeRequest(String url, String method, String body) throws IOException, InterruptedException {

        HttpRequest request = createRequest(url, method, body);
        return sendRequest(request).body();
    }

    /**
     * Make an HTTP authenticated request to the given URL with the specified method and body
     *
     * @param url       the URL to make the request to
     * @param method    the HTTP method to use (e.g., "GET", "POST", etc.)
     * @param body      the body of the request (for POST, PUT, etc.)
     * @param authToken the authentication token for the request
     * @return the response body as a string
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public static String makeRequest(String url, String method, String body, String authToken) throws IOException, InterruptedException {

        HttpRequest request = createRequest(url, method, body, authToken);
        return sendRequest(request).body();
    }


    /**
     * Create an HTTP request with the given URL, method, and body
     *
     * @param url    the url of the request
     * @param method the method of the request
     * @param body   the body of the request
     * @return the created HttpRequest object
     */
    public static HttpRequest createRequest(String url, String method, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
    }


    /**
     * Create an HTTP authenticated request with the given URL, method, and body
     *
     * @param url       the url of the request
     * @param method    the method of the request
     * @param body      the body of the request
     * @param authToken the authentication token for the request
     * @return the created HttpRequest object
     */
    public static HttpRequest createRequest(String url, String method, String body, String authToken) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .build();
    }


    /**
     * Send an HTTP request and return the response body
     *
     * @param request the HTTP request to send
     * @return the response body as a string
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public static HttpResponse<String> sendRequest(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
