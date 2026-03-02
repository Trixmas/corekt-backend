package lab.corekt.backend.modules.project.parsedData.repository;

import lab.corekt.backend.modules.project.parsedData.entity.ParsedDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParsedDataRepository extends JpaRepository<ParsedDataEntity, Integer> {

}

