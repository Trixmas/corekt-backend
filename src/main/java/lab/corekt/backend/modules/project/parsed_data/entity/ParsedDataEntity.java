package lab.corekt.backend.modules.project.parsed_data.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.enums.DataType;
import lab.corekt.backend.modules.project.data_link.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.file.entity.FileEntity;
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

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "parsed_data_id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataType dataType;

    @Column(nullable = false, length = 100)
    private String content;


    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @OneToMany(mappedBy = "sourceParsedData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DataLinkEntity> outgoingLinks;

    @OneToMany(mappedBy = "targetParsedData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DataLinkEntity> incomingLinks;

    // Constructeurs
    public ParsedDataEntity() {
    }

    public ParsedDataEntity(DataType dataType, String content, FileEntity file) {
        this.dataType = dataType;
        this.content = content;
        this.file = file;
    }
}

