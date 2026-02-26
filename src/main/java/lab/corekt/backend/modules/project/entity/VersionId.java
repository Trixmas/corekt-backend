package lab.corekt.backend.modules.project.entity;

import java.io.Serializable;
import java.util.Objects;

// Composite primary key for the Version entity
public class VersionId implements Serializable {

    private Integer versionId;
    private Integer project; // Must match the property name in VersionEntity

    public VersionId() {
    }

    public VersionId(Integer versionId, Integer project) {
        this.versionId = versionId;
        this.project = project;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VersionId versionId1 = (VersionId) o;
        return Objects.equals(versionId, versionId1.versionId) && Objects.equals(project, versionId1.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionId, project);
    }
}

