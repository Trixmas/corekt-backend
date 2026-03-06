package lab.corekt.backend.modules.project.report.repository;

import lab.corekt.backend.modules.project.report.entity.AIAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AIAnalysisRepository extends JpaRepository<AIAnalysisEntity, UUID> {

}
