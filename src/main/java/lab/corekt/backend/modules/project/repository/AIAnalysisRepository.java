package lab.corekt.backend.modules.project.repository;

import lab.corekt.backend.modules.project.entity.AIAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIAnalysisRepository extends JpaRepository<AIAnalysisEntity, Integer> {
    Optional<AIAnalysisEntity> findByFile_Id(UUID fileId);
}

