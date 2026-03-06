package lab.corekt.backend.modules.project.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "project")
@EntityListeners(AuditingEntityListener.class)
public class ProjectEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "project_id")
    private UUID id;

    @Column(name = "project_title", nullable = false, length = 50)
    private String title;

    @Column(nullable = false)
    private LocalDate createdAt;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;


    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<VersionEntity> versions = new ArrayList<>();

    protected ProjectEntity() {
        this.createdAt = LocalDate.now();
    }

    public ProjectEntity(String title, UserEntity owner) {
        this.title = title;
        this.owner = owner;
        this.createdAt = LocalDate.now();
        VersionEntity firstVersion = new VersionEntity("Première version");
        addVersion(firstVersion);
    }

    public void addVersion(VersionEntity version) {
        versions.add(version);
        version.setProject(this);
    }
}

