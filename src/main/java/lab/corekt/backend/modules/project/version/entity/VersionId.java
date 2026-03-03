package lab.corekt.backend.modules.project.version.entity;

import lab.corekt.backend.modules.project.project.entity.ProjectEntity;

import java.io.Serializable;
import java.util.Objects;

public class VersionId implements Serializable {
    private Integer id;
    private ProjectEntity project;

    public VersionId() {
    }

    public VersionId(Integer id, ProjectEntity project) {
        this.id = id;
        this.project = project;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionId versionId = (VersionId) o;
        return Objects.equals(id, versionId.id) && Objects.equals(project.getId(), versionId.project.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, project);
    }
}
