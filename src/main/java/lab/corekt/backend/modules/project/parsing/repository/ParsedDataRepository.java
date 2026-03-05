package lab.corekt.backend.modules.project.parsing.repository;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ParsedDataRepository extends JpaRepository<ParsedDataEntity, UUID> {

}
