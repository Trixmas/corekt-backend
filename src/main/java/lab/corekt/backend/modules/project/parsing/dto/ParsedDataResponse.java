package lab.corekt.backend.modules.project.parsing.dto;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.upload.entity.FileEntity;

public record ParsedDataResponse(
    String fileName,
    FileType fileType,
    DataType dataType,
    String content
) {
    public ParsedDataResponse(ParsedDataEntity parsedData) {
        FileEntity file = parsedData.getFile();
        this(
            file.getName(),
            file.getType(),
            parsedData.getDataType(),
            parsedData.getContent()
        );
    }
}
