package lab.corekt.backend.modules.project.parsing.repository;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ParsedDataRepository extends JpaRepository<ParsedDataEntity, UUID> {

    @Query("select p from ParsedDataEntity p join p.file f join f.version v where v = :version")
    List<ParsedDataEntity> findByVersion(VersionEntity version);
}
