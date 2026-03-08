package lab.corekt.backend.modules.project.versions.repository;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, UUID> {
    List<VersionEntity> findFirstVersionByProject(ProjectEntity project);

    Optional<VersionEntity> findLatestVersionByProject(ProjectEntity project);

    Optional<VersionEntity> findVersionByProjectAndVersionId(ProjectEntity project, UUID versionId);

    int countByProjectId(ProjectEntity project);
}
