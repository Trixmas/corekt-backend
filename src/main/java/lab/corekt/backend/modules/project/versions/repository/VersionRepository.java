package lab.corekt.backend.modules.project.versions.repository;

import aj.org.objectweb.asm.commons.Remapper;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, UUID> {
    List<VersionEntity> findFirstVersionByProjectId(UUID projectId);

    Optional<VersionEntity> findLatestVersionByProjectId(UUID projectId);

    Optional<VersionEntity> findVersionByProjectIdAndVersionId(UUID projectId, UUID versionId);

    int countByProjectId(UUID projectId);
}