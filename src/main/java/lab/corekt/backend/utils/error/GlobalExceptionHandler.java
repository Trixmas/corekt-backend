package lab.corekt.backend.utils.error;

import jakarta.validation.ConstraintViolationException;
import lab.corekt.backend.modules.taiga.exception.TaigaAuthException;
import lab.corekt.backend.modules.taiga.exception.TaigaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.ldap.CommunicationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TaigaAuthException.class)
    public ResponseEntity<ErrorResponse> handleTaigaAuthException(TaigaAuthException e, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    @ExceptionHandler(TaigaException.class)
    public ResponseEntity<ErrorResponse> handleTaigaException(TaigaException e, WebRequest request) {
        return build(HttpStatus.BAD_GATEWAY, e.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            assert errorMessage != null;
            errors.put(HtmlUtils.htmlEscape(fieldName), HtmlUtils.htmlEscape(errorMessage));
        });
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getConstraintViolations().forEach(violation ->
            errors.put(
                HtmlUtils.htmlEscape(violation.getPropertyPath().toString()),
                HtmlUtils.htmlEscape(violation.getMessage())
            )
        );
        return build(HttpStatus.BAD_REQUEST, "Constraint violation", errors, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e, WebRequest request) {
        logger.error("Data integrity violation: ", e);
        return build(HttpStatus.CONFLICT, "Data integrity violation: " + e.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, WebRequest request) {
        logger.warn("Malformed or missing request body: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed or missing request body", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, HtmlUtils.htmlEscape(e.getMessage()), request);
    }

    @ExceptionHandler(CommunicationException.class)
    public ResponseEntity<ErrorResponse> handleLdapCommunicationException(CommunicationException e, WebRequest request) {
        logger.error("LDAP communication failure: ", e);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Authentication service is currently unavailable", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, WebRequest request) {
        String message = "Invalid value '%s' for parameter '%s': expected %s".formatted(
            e.getValue(),
            e.getName(),
            e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown type"
        );
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(WebRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e, WebRequest request) {
        String message = e.getReason() != null ? e.getReason() : e.getMessage();
        return build(HttpStatus.valueOf(e.getStatusCode().value()), message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception e, WebRequest request) {
        logger.error("Unhandled exception occurred: ", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, WebRequest request) {
        return build(status, message, null, request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, Map<String, String> errors, WebRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(
            status.value(),
            message,
            errors,
            sanitizePath(request.getDescription(false)),
            Instant.now()
        ));
    }

    private String sanitizePath(String description) {
        return HtmlUtils.htmlEscape(description.replace("uri=", ""));
    }
}
