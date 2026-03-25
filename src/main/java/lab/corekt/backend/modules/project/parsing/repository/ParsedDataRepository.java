package lab.corekt.backend.modules.project.parsing.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;

public interface ParsedDataRepository extends JpaRepository<ParsedDataEntity, UUID> {

    // @Query("SELECT p FROM ParsedDataEntity p join FileEntity f on p.file join
    // VersionEntity v on f.version = v WHERE v = :version")
    // List<ParsedDataEntity> findByVersion(VersionEntity version);

    @Query("select p from ParsedDataEntity p join p.file f join f.version v where v = :version")
    List<ParsedDataEntity> findByVersion(VersionEntity version);

    List<ParsedDataEntity> findByFile(FileEntity file);
}
