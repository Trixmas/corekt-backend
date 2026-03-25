package lab.corekt.backend.modules.project.parsing.repository;

import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DataLinkRepository extends JpaRepository<DataLinkEntity, UUID> {

    @Query("select d from DataLinkEntity d left join d.sourceParsedData p join p.file f join f.version v where v.id = ?1")
    List<DataLinkEntity> findByVersionId(UUID versionId);
}
