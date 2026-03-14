package lab.corekt.backend.modules.taiga.exception;

public class TaigaAuthException extends RuntimeException {
    public TaigaAuthException(String message) {
        super(message);
    }
}
