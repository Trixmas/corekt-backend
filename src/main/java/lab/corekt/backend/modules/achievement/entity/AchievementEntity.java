package lab.corekt.backend.modules.achievement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "achievement")
@EntityListeners(AuditingEntityListener.class)
public class AchievementEntity {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private boolean hidden;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String criteria;

    @OneToMany(mappedBy = "achievement")
    private List<UserAchievementEntity> userAchievements;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public AchievementEntity() {
    }

    public AchievementEntity(String id, String name, String description, boolean hidden, String criteria) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.hidden = hidden;
        this.criteria = criteria;
    }
}
