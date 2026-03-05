package lab.corekt.backend.modules.project.parsing.repository;

import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DataLinkRepository extends JpaRepository<DataLinkEntity, UUID> {

}
