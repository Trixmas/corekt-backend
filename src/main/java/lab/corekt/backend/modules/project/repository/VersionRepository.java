package lab.corekt.backend.modules.project.repository;

import lab.corekt.backend.modules.project.entity.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, Integer> {
    List<VersionEntity> findByProject_ProjectId(Integer projectId);

    List<VersionEntity> findByProject_ProjectIdAndVersionTitleContainingIgnoreCase(Integer projectId, String versionTitle);
}

