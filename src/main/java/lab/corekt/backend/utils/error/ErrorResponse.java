package lab.corekt.backend.utils.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    int status,
    String message,
    Map<String, String> errors,
    String path,
    Instant timestamp
) {
}
