package lab.corekt.backend.modules.project.parsing.repository;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParsedDataRepository extends JpaRepository<ParsedDataEntity, UUID> {

    List<ParsedDataEntity> findByFile(FileEntity file);
}
