package lab.corekt.backend.modules.project.parsed_data.repository;

import lab.corekt.backend.modules.project.parsed_data.entity.ParsedDataEntity;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParsedDataRepository extends JpaRepository<ParsedDataEntity, UUID> {

}

