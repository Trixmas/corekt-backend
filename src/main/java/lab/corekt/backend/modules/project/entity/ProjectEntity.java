package lab.corekt.backend.modules.project.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "project")
@EntityListeners(AuditingEntityListener.class)
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Seq_Project_Gen")
    @SequenceGenerator(name = "Seq_Project_Gen", sequenceName = "seq_project_id", allocationSize = 1)
    private Integer projectId;

    @Column(nullable = false, length = 50)
    private String projectTitle;

    @Column(nullable = false)
    private LocalDate createdAt;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;


    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<VersionEntity> versions;

    // Constructeurs
    public ProjectEntity() {
        this.createdAt = LocalDate.now();
    }

    public ProjectEntity(String projectTitle, UserEntity user) {
        this.projectTitle = projectTitle;
        this.owner = user;
        this.createdAt = LocalDate.now();
    }
}

