package lab.corekt.backend.modules.project.repository;

import lab.corekt.backend.modules.project.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    List<FileEntity> findByVersionIdAndProjectId(Integer versionId, UUID projectId);

    List<FileEntity> findByNameContainingIgnoreCase(String name);
}

