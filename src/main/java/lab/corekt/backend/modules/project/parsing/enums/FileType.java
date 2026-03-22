package lab.corekt.backend.modules.project.parsing.enums;

import org.jspecify.annotations.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.FileSystemException;

public enum FileType {
    BPMN,
    US,
    INTERVIEW,
    MCD;

    public boolean isValidFile(MultipartFile file) {
        try {
            String extension = getExtension(file).toLowerCase();
            return switch (this) {
                case BPMN -> extension.equals("bpmn");
                case INTERVIEW -> extension.equals("pdf");
                case MCD -> extension.equals("mcd");
                default -> false;
            };
        } catch (FileSystemException e) {
            return false;
        }
    }

    private String getExtension(@NonNull MultipartFile file) throws FileSystemException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) throw new FileSystemException("File name cannot be null");
        return file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
    }
}
