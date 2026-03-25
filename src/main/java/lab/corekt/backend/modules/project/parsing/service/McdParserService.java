package lab.corekt.backend.modules.project.parsing.service;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class McdParserService {

    private final ParsedDataRepository parsedDataRepository;

    public void processFile(FileEntity fileEntity) throws Exception {
        McdParserReader reader = new McdParserReader(fileEntity);
        reader.parseFile();

        List<ParsedDataEntity> parsedDataEntityList = new ArrayList<>();

        parsedDataEntityList.addAll(convertToParsedDataEntities(reader.getEntities(), DataType.ENTITY, fileEntity));
        parsedDataEntityList.addAll(convertToParsedDataEntities(reader.getRelations(), DataType.RELATION, fileEntity));
        parsedDataRepository.saveAll(parsedDataEntityList);
    }

    private List<ParsedDataEntity> convertToParsedDataEntities(List<String> names, DataType dataType, FileEntity fileEntity) {
        return names.stream().map(name -> createParsedDataEntity(name, dataType, fileEntity)).toList();
    }

    private ParsedDataEntity createParsedDataEntity(String content, DataType dataType, FileEntity fileEntity) {
        ParsedDataEntity parsedDataEntity = new ParsedDataEntity();

        if (content.length() > ParsedDataEntity.MAX_CONTENT_LENGTH) {
            String newContent = content.substring(0, ParsedDataEntity.MAX_CONTENT_LENGTH);
            parsedDataEntity.setContent(newContent);
        } else {
            parsedDataEntity.setContent(content);
        }

        parsedDataEntity.setDataType(dataType);
        parsedDataEntity.setFile(fileEntity);

        return parsedDataEntity;
    }
}
