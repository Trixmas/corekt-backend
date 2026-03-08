package lab.corekt.backend.modules.project.versions.repository;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, VersionId> {
    Optional<VersionEntity> findByIdAndProject(Integer id, ProjectEntity project);

    List<VersionEntity> findAllByProject(ProjectEntity project);

    void deleteByIdAndProject(Integer versionId, ProjectEntity project);
}
