package lab.corekt.backend.modules.project.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;

import java.time.Instant;
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
    private String description;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<VersionEntity> versions;

    protected ProjectEntity() {

    }

    public ProjectEntity(String title, UserEntity user) {
        this.title = title;
        this.owner = user;
    }
}