package lab.corekt.backend.modules.project.repository;

import lab.corekt.backend.modules.project.entity.ParsedDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParsedDataRepository extends JpaRepository<ParsedDataEntity, Integer> {
    List<ParsedDataEntity> findByFile_FileId(Integer fileId);

    List<ParsedDataEntity> findByContentContainingIgnoreCase(String content);
}

