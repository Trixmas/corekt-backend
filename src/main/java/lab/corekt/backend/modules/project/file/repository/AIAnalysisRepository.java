package lab.corekt.backend.modules.project.file.repository;

import lab.corekt.backend.modules.project.file.entity.AIAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIAnalysisRepository extends JpaRepository<AIAnalysisEntity, Integer> {
}

