package lab.corekt.backend.modules.project.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.project.enums.DataType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "parsed_data")
@EntityListeners(AuditingEntityListener.class)
public class ParsedDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parsed_data_id")
    private Integer parsedDataId;

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

