package lab.corekt.backend.modules.project.file.dto;

import java.util.UUID;

import lab.corekt.backend.modules.enums.FileType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadResponse {
    private UUID fileId;
    private String fileName;
    private FileType fileType;
}
