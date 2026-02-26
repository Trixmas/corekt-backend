package lab.corekt.backend.modules.project.repository;

import lab.corekt.backend.modules.project.entity.DataLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataLinkRepository extends JpaRepository<DataLinkEntity, Integer> {
    List<DataLinkEntity> findBySourceParsedDataId(Integer sourceParsedDataId);

    List<DataLinkEntity> findByTargetParsedDataId(Integer targetParsedDataId);

    List<DataLinkEntity> findBySourceParsedDataIdAndTargetParsedDataId(Integer sourceParsedDataId, Integer targetParsedDataId);
}

