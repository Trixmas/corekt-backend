package lab.corekt.backend.modules.project.versions.repository;

import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, VersionId> {

}

