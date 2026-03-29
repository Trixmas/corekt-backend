package lab.corekt.backend.modules.project.versions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.dto.DataLinkResponse;
import lab.corekt.backend.modules.project.parsing.dto.ParsedDataResponse;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.enums.Severity;
import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lab.corekt.backend.modules.project.versions.dto.VersionDetailsResponse;

@ExtendWith(MockitoExtension.class)
class VersionPdfExportServiceTest {

    @Mock
    private VersionService versionService;

    @Mock
    private ProjectEntity project;

    @InjectMocks
    private VersionPdfExportService versionPdfExportService;

    @Test
    @DisplayName("Should include priority data link details in generated PDF")
    void generateVersionSummaryPdfIncludesPriorityDataLinkDetails() throws IOException {
        UUID versionId = UUID.randomUUID();

        VersionDetailsResponse version = new VersionDetailsResponse();
        version.setTitle("Sprint paiement");
        version.setVersion(2);
        version.setStatus(VersionStatus.COMPLETED);
        version.setDataLinks(List.of(
            new DataLinkResponse(
                UUID.randomUUID(),
                MatchType.MISSING,
                Severity.CRITICAL,
                32,
                "Le workflow BPMN ne couvre pas la validation du paiement.",
                "Ajouter une etape BPMN pour la validation du paiement.",
                new ParsedDataResponse("interview.pdf", FileType.INTERVIEW, DataType.ACTION, "valider le paiement"),
                new ParsedDataResponse("processus.bpmn", FileType.BPMN, DataType.FLOW, "payment validation")
            ),
            new DataLinkResponse(
                UUID.randomUUID(),
                MatchType.EXACT,
                Severity.MINOR,
                95,
                "Correspondance directe.",
                "Aucune action requise.",
                new ParsedDataResponse("user-story.txt", FileType.US, DataType.US, "gerer le compte"),
                new ParsedDataResponse("interview.pdf", FileType.INTERVIEW, DataType.ACTION, "gerer le compte")
            ),
            new DataLinkResponse(
                UUID.randomUUID(),
                MatchType.SEMANTIC,
                Severity.MAJOR,
                75,
                "Similarite semantique de 75% entre la valeur 'Le manager' du fichier 'INTERVIEW' et la valeur 'Utilisateur' du fichier 'ProcessusPrincipal.bpmn'.",
                "Verifier si ces acteurs doivent etre fusionnes.",
                new ParsedDataResponse("interview.pdf", FileType.INTERVIEW, DataType.ACTOR, "Le manager"),
                new ParsedDataResponse("ProcessusPrincipal.bpmn", FileType.BPMN, DataType.ACTOR, "Utilisateur")
            )
        ));

        when(project.getTitle()).thenReturn("Projet Corekt");
        when(versionService.getVersion(project, versionId)).thenReturn(version);

        byte[] pdf = versionPdfExportService.generateVersionSummaryPdf(project, versionId);

        assertThat(pdf).isNotEmpty();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);

            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            assertThat(text).contains("Rapport Corekt");
            assertThat(text).contains("Projet Corekt");
            assertThat(text).contains("Sprint paiement");
            assertThat(text).contains("Data links prioritaires");
            assertThat(text).doesNotContain("Correspondances exactes");
            assertThat(text).contains("MISSING");
            assertThat(text).contains("CRITICAL");
            assertThat(text).contains("Confiance 32/100");
            assertThat(text).contains("32/100");
            assertThat(text).contains("Source");
            assertThat(text).contains("Cible");
            assertThat(text).contains("INTERVIEW / ACTION");
            assertThat(text).contains("interview.pdf");
            assertThat(text).contains("BPMN / FLOW");
            assertThat(text).contains("processus.bpmn");
            assertThat(text).contains("Extrait");
            assertThat(text).contains("valider le paiement");
            assertThat(text).contains("Ajouter une etape BPMN pour la validation du paiement.");
            assertThat(text).contains("Similarite semantique de 75% entre la valeur 'Le manager' du fichier 'INTERVIEW'");
            assertThat(text).contains("la valeur 'Utilisateur'");
            assertThat(text).contains("'ProcessusPrincipal.bpmn'.");
            assertThat(text).doesNotContain("EXACT");
            assertThat(text).doesNotContain("Confiance 95/100");
            assertThat(text).doesNotContain("user-story.txt");
            assertThat(text).doesNotContain("Aucune action requise.");
            assertThat(text).doesNotContain("n/a");
            assertThat(text).doesNotContain("Autres data links");
        }
    }
}
