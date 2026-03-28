package lab.corekt.backend.modules.project.versions.dto;

import lab.corekt.backend.modules.project.parsing.dto.DataLinkResponse;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
public class VersionDetailsResponse extends VersionResponse {
    private List<DataLinkResponse> dataLinks;
    private AiAnalysisResponse iaAnalysis;

    public VersionDetailsResponse() {
        super();
        this.dataLinks = new ArrayList<>();
        this.iaAnalysis = new AiAnalysisResponse();

    }

    public VersionDetailsResponse(VersionEntity version, List<DataLinkEntity> dataLinks) {
        super(version, dataLinks);
        this.dataLinks = dataLinks.stream().map(DataLinkResponse::new).toList();
        this.iaAnalysis = version.getFiles().stream()
                .map(FileEntity::getAiAnalysis)
                .filter(Objects::nonNull)
                .findFirst()
                .map(AiAnalysisResponse::new)
                .orElse(new AiAnalysisResponse());
    }
}
