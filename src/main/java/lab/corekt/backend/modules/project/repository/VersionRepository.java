package lab.corekt.backend.modules.project.repository;

import lab.corekt.backend.modules.project.entity.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, Integer> {
    List<VersionEntity> findByProject_Id(UUID projectId);

    List<VersionEntity> findByProject_IdAndVersionTitleContainingIgnoreCase(UUID projectId, String versionTitle);
}

