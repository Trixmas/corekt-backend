package lab.corekt.backend.modules.upload.dto;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FileUploadResponse {
    private UUID fileId;
    private String fileName;
    private FileType fileType;
}
