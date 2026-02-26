package lab.corekt.backend.modules.project.entity;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
public class VersionId implements Serializable {
    private Integer versionId;
    private UUID project;
    public VersionId() {
    }
    public VersionId(Integer versionId, UUID project) {
        this.versionId = versionId;
        this.project = project;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionId that = (VersionId) o;
        return Objects.equals(versionId, that.versionId) && Objects.equals(project, that.project);
    }
    @Override
    public int hashCode() {
        return Objects.hash(versionId, project);
    }
}
