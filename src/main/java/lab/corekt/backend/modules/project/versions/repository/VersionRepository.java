package lab.corekt.backend.modules.project.versions.repository;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, UUID> {
    List<VersionEntity> findByProject(ProjectEntity project);

    Optional<VersionEntity> findTopByProjectOrderByVersionNumberDesc(ProjectEntity project);

    Optional<VersionEntity> findByProjectAndId(ProjectEntity project, UUID id);

    long countByProjectOwnerAndStatus(UserEntity owner, VersionStatus status);
}