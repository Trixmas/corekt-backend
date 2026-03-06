package lab.corekt.backend.modules.project.versions.repository;

import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, UUID> {

}