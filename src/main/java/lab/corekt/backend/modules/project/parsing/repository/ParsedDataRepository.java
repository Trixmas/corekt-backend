package lab.corekt.backend.modules.project.parsing.repository;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParsedDataRepository extends JpaRepository<ParsedDataEntity, UUID> {

//    @Query("SELECT p FROM ParsedDataEntity p join FileEntity f on p.file join VersionEntity v on f.version = v WHERE v = :version")
//    List<ParsedDataEntity> findByVersion(VersionEntity version);

    List<ParsedDataEntity> findByFile(FileEntity file);
}
