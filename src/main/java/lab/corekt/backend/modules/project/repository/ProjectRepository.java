package lab.corekt.backend.modules.project.repository;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    List<ProjectEntity> findByOwner(UserEntity owner);
    Optional<ProjectEntity> findByIdAndOwner(UUID id, UserEntity owner);
}
