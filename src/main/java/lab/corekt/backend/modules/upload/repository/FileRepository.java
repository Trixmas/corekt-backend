package lab.corekt.backend.modules.upload.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    boolean existsByVersionAndType(VersionEntity version, FileType type);
}

