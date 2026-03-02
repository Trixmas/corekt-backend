package lab.corekt.backend.modules.project.repository;

import lab.corekt.backend.modules.project.entity.DataLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataLinkRepository extends JpaRepository<DataLinkEntity, Integer> {
}

