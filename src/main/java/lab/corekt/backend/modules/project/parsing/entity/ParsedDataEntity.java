package lab.corekt.backend.modules.project.parsing.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "parsed_data")
@EntityListeners(AuditingEntityListener.class)
public class ParsedDataEntity {

    public static final int MAX_CONTENT_LENGTH = 500;

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "parsed_data_id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataType dataType;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @OneToMany(mappedBy = "sourceParsedData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DataLinkEntity> outgoingLinks;

    @OneToMany(mappedBy = "targetParsedData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DataLinkEntity> incomingLinks;

    public ParsedDataEntity() {
    }

    public ParsedDataEntity(DataType dataType, String content) {
        this.dataType = dataType;
        this.content = content;
    }

    public ParsedDataEntity(DataType dataType, String actor, FileEntity relatedFile) {
        this.dataType = dataType;
        this.content = actor;
        this.file = relatedFile;
    }
}
