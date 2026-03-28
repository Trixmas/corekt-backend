package lab.corekt.backend.modules.upload.event;

import java.util.UUID;

public record FileUploadedEvent(UUID fileId) {
}

