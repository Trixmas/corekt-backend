package lab.corekt.backend.modules.upload.dto;

import java.util.UUID;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadResponse {
    private UUID fileId;
    private String fileName;
    private FileType fileType;
}
