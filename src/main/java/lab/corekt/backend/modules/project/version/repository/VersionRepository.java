package lab.corekt.backend.modules.project.version.repository;

import lab.corekt.backend.modules.project.version.entity.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, Integer> {

}

