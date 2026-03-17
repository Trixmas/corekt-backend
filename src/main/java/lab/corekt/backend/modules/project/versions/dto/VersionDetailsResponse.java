package lab.corekt.backend.modules.project.versions.dto;

import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class VersionDetailsResponse extends VersionResponse {
    private List<DataLinkEntity> dataLinks;

    public VersionDetailsResponse() {
        super();
        this.dataLinks = new ArrayList<>();
    }

    public VersionDetailsResponse(VersionEntity version, List<DataLinkEntity> dataLinks) {
        super(version);
        this.dataLinks = dataLinks;
    }
}
