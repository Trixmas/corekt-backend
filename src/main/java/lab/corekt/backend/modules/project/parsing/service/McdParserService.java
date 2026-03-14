package lab.corekt.backend.modules.project.parsing.service;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class McdParserService {

    private final ParsedDataRepository parsedDataRepository;

    public McdParserService(ParsedDataRepository parsedDataRepository) {
        this.parsedDataRepository = parsedDataRepository;
    }

    public void processFile(FileEntity fileEntity) throws Exception{
        McdParserReader reader = new McdParserReader(fileEntity);
        reader.parseFile();

        List<ParsedDataEntity> parsedDataEntityList = new ArrayList<>();

        for(String name : reader.getEntities()) {
            parsedDataEntityList.add(createParsedDataEntity(name, DataType.ENTITY, fileEntity));
        }
        for(String name : reader.getRelations()) {
            parsedDataEntityList.add(createParsedDataEntity(name, DataType.RELATION, fileEntity));
        }

        parsedDataRepository.saveAll(parsedDataEntityList);
    }

    private ParsedDataEntity createParsedDataEntity(String content, DataType dataType, FileEntity fileEntity) {
        ParsedDataEntity parsedDataEntity = new ParsedDataEntity();

        if (content.length() > ParsedDataEntity.MAX_CONTENT_LENGTH) {
            String newContent = content.substring(0, ParsedDataEntity.MAX_CONTENT_LENGTH);
            parsedDataEntity.setContent(newContent);
        }  else {
            parsedDataEntity.setContent(content);
        }

        parsedDataEntity.setDataType(dataType);
        parsedDataEntity.setFile(fileEntity);

        return parsedDataEntity;
    }
}
