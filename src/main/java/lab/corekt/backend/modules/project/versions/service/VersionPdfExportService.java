package lab.corekt.backend.modules.project.versions.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.dto.DataLinkResponse;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.versions.dto.VersionDetailsResponse;

@Service
public class VersionPdfExportService {

        private static final float PAGE_MARGIN = 50f;
        private static final float LINE_HEIGHT = 18f;

        private final VersionService versionService;

        public VersionPdfExportService(VersionService versionService) {
                this.versionService = versionService;
        }

        public byte[] generateVersionSummaryPdf(ProjectEntity project, UUID versionId) {
                VersionDetailsResponse version = versionService.getVersion(project, versionId);

                long exactCount = countByMatch(version, MatchType.EXACT);
                long semanticCount = countByMatch(version, MatchType.SEMANTIC);
                long missingCount = countByMatch(version, MatchType.MISSING);

                try (PDDocument document = new PDDocument();
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        PDPage page = new PDPage(PDRectangle.A4);
                        document.addPage(page);

                        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                                float startY = page.getMediaBox().getHeight() - PAGE_MARGIN;
                                float currentY = startY;

                                currentY = writeLine(contentStream, Standard14Fonts.FontName.HELVETICA_BOLD, 18,
                                                "Rapport Corekt", PAGE_MARGIN, currentY);
                                currentY -= 10;
                                currentY = writeLine(contentStream, Standard14Fonts.FontName.HELVETICA, 12,
                                                "Projet : " + project.getTitle(), PAGE_MARGIN, currentY);
                                currentY = writeLine(contentStream, Standard14Fonts.FontName.HELVETICA, 12,
                                                "Version : " + version.getTitle() + " (#" + version.getVersion() + ")",
                                                PAGE_MARGIN, currentY);
                                currentY = writeLine(contentStream, Standard14Fonts.FontName.HELVETICA, 12,
                                                "Statut : " + version.getStatus(), PAGE_MARGIN, currentY);
                                currentY = writeLine(contentStream, Standard14Fonts.FontName.HELVETICA, 12,
                                                "Export genere le : " + Instant.now(), PAGE_MARGIN, currentY);

                                currentY -= 10;
                                currentY = writeLine(contentStream, Standard14Fonts.FontName.HELVETICA_BOLD, 14,
                                                "Resume", PAGE_MARGIN, currentY);
                                currentY = writeLine(contentStream, Standard14Fonts.FontName.HELVETICA, 12,
                                                "Nombre total de liens : " + version.getDataLinks().size(), PAGE_MARGIN,
                                                currentY);
                                currentY = writeLine(contentStream, Standard14Fonts.FontName.HELVETICA, 12,
                                                "Correspondances exactes : " + exactCount, PAGE_MARGIN, currentY);
                                currentY = writeLine(contentStream, Standard14Fonts.FontName.HELVETICA, 12,
                                                "Correspondances semantiques : " + semanticCount, PAGE_MARGIN,
                                                currentY);
                                currentY = writeLine(contentStream, Standard14Fonts.FontName.HELVETICA, 12,
                                                "Elements manquants : " + missingCount, PAGE_MARGIN, currentY);
                        }

                        document.save(outputStream);
                        return outputStream.toByteArray();
                } catch (IOException e) {
                        throw new IllegalStateException("Failed to generate version PDF export", e);
                }
        }

        private long countByMatch(VersionDetailsResponse version, MatchType matchType) {
                return version.getDataLinks()
                                .stream()
                                .map(DataLinkResponse::match)
                                .filter(matchType::equals)
                                .count();
        }

        private float writeLine(
                        PDPageContentStream contentStream,
                        Standard14Fonts.FontName font,
                        float fontSize,
                        String text,
                        float x,
                        float y) throws IOException {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(font), fontSize);
                contentStream.newLineAtOffset(x, y);
                contentStream.showText(text);
                contentStream.endText();
                return y - LINE_HEIGHT;
        }
}
