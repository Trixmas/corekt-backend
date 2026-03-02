package lab.corekt.backend.modules.project.version.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class VersionId implements Serializable {
    private Integer id;
    private UUID project;

    public VersionId() {
    }

    public VersionId(Integer id, UUID project) {
        this.id = id;
        this.project = project;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionId that = (VersionId) o;
        return Objects.equals(id, that.id) && Objects.equals(project, that.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, project);
    }
}
