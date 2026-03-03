package lab.corekt.backend.modules.project.data_link.repository;

import lab.corekt.backend.modules.project.data_link.entity.DataLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DataLinkRepository extends JpaRepository<DataLinkEntity, UUID> {
}

