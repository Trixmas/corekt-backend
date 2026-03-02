package lab.corekt.backend.modules.project.repository;

import lab.corekt.backend.modules.project.entity.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, Integer> {

}

