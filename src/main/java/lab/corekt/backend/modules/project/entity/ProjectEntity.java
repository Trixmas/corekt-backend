package lab.corekt.backend.modules.project.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;

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

    @OneToMany(mappedBy = "project", cascade = { CascadeType.PERSIST,
            CascadeType.REMOVE }, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VersionEntity> versions = new ArrayList<>();

    protected ProjectEntity() {
    }

    public ProjectEntity(String title, String description, UserEntity user) {
        this.title = title;
        this.description = description;
        this.owner = user;
        versions.add(new VersionEntity(this, "Première version"));
    }
}