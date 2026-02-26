package lab.corekt.backend.modules.project.repository;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Integer> {
    List<ProjectEntity> findByOwner(UserEntity owner);

    List<ProjectEntity> findByTitleContainingIgnoreCase(String projectTitle);
}

