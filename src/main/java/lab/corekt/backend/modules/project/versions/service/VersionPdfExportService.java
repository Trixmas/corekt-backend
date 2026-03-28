package lab.corekt.backend.modules.project.versions.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.dto.DataLinkResponse;
import lab.corekt.backend.modules.project.parsing.dto.ParsedDataResponse;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.enums.Severity;
import lab.corekt.backend.modules.project.versions.dto.VersionDetailsResponse;


/**
 * La génération du pdf étant complexe à stylisée, nous nous sommes aidé d'un LLM
 */
@Service
public class VersionPdfExportService {

    private static final float PAGE_MARGIN = 42f;
    private static final float MIN_BOTTOM_MARGIN = 54f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float WRITABLE_WIDTH = PAGE_WIDTH - (PAGE_MARGIN * 2);
    private static final float DEFAULT_LINE_HEIGHT = 15f;
    private static final float BLOCK_SPACING = 12f;
    private static final float SECTION_SPACING = 18f;
    private static final float CARD_RADIUS = 0f;
    private static final float METRIC_CARD_HEIGHT = 66f;
    private static final float METRIC_CARD_GAP = 10f;
    private static final float METRIC_CARD_WIDTH = (WRITABLE_WIDTH - (METRIC_CARD_GAP * 2)) / 3f;
    private static final float DATA_LINK_ACCENT_WIDTH = 8f;
    private static final float DATA_LINK_PADDING = 16f;
    private static final float CHIP_HEIGHT = 18f;
    private static final float CHIP_GAP = 6f;
    private static final float DETAIL_PANEL_GAP = 12f;
    private static final float DETAIL_PANEL_PADDING = 12f;
    private static final float PREVIEW_BOX_PADDING = 8f;
    private static final int MAX_PREVIEW_LENGTH = 220;
    private static final Pattern JUSTIFICATION_HIGHLIGHT_PATTERN = Pattern.compile("'[^']+'|\\b\\d+%\\b");
    private static final DateTimeFormatter PDF_TIMESTAMP_FORMAT = DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault());

    private static final Color PAGE_BACKGROUND = new Color(252, 252, 251);
    private static final Color TEXT_PRIMARY = new Color(34, 39, 46);
    private static final Color TEXT_SECONDARY = new Color(97, 106, 117);
    private static final Color BORDER_COLOR = new Color(221, 226, 232);
    private static final Color CARD_BACKGROUND = new Color(248, 249, 251);
    private static final Color MUTED_BACKGROUND = new Color(241, 244, 247);
    private static final Color HERO_BACKGROUND = new Color(28, 45, 68);
    private static final Color HERO_ACCENT = new Color(112, 160, 183);
    private static final Color HERO_TEXT = new Color(248, 249, 251);
    private static final Color HERO_MUTED = new Color(201, 212, 224);
    private static final Color SECTION_COLOR = new Color(58, 90, 122);
    private static final Color EXACT_COLOR = new Color(74, 135, 106);
    private static final Color SEMANTIC_COLOR = new Color(176, 132, 61);
    private static final Color MISSING_COLOR = new Color(163, 79, 72);
    private static final Color MINOR_COLOR = new Color(86, 113, 140);
    private static final Color MAJOR_COLOR = new Color(182, 125, 54);
    private static final Color CRITICAL_COLOR = new Color(156, 64, 64);

    private static final Comparator<DataLinkResponse> DATA_LINK_PRIORITY = Comparator
        .comparingInt(VersionPdfExportService::priorityBucket)
        .reversed()
        .thenComparingInt(VersionPdfExportService::severityRank)
        .reversed()
        .thenComparingInt(VersionPdfExportService::matchRank)
        .reversed();

    private final VersionService versionService;

    public VersionPdfExportService(VersionService versionService) {
        this.versionService = versionService;
    }

    public byte[] generateVersionSummaryPdf(ProjectEntity project, UUID versionId) {
        VersionDetailsResponse version = versionService.getVersion(project, versionId);
        Instant generatedAt = Instant.now();
        List<DataLinkResponse> sortedDataLinks = safeDataLinks(version).stream()
            .sorted(DATA_LINK_PRIORITY)
            .toList();
        List<DataLinkResponse> priorityLinks = sortedDataLinks.stream()
            .filter(VersionPdfExportService::isPriorityLink)
            .toList();
        List<DataLinkResponse> secondaryLinks = sortedDataLinks.stream()
            .filter(link -> !isPriorityLink(link))
            .toList();

        List<MetricCard> metrics = List.of(
            new MetricCard("Nombre total de liens", String.valueOf(sortedDataLinks.size()), SECTION_COLOR),
            new MetricCard("Elements manquants", String.valueOf(countByMatch(sortedDataLinks, MatchType.MISSING)), MISSING_COLOR),
            new MetricCard("Severite critique", String.valueOf(countBySeverity(sortedDataLinks, Severity.CRITICAL)), CRITICAL_COLOR),
            new MetricCard("Severite majeure", String.valueOf(countBySeverity(sortedDataLinks, Severity.MAJOR)), MAJOR_COLOR),
            new MetricCard("Correspondances semantiques", String.valueOf(countByMatch(sortedDataLinks, MatchType.SEMANTIC)), SEMANTIC_COLOR),
            new MetricCard("Correspondances exactes", String.valueOf(countByMatch(sortedDataLinks, MatchType.EXACT)), EXACT_COLOR),
            new MetricCard("Severite mineure", String.valueOf(countBySeverity(sortedDataLinks, Severity.MINOR)), MINOR_COLOR)
        );

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (PdfLayoutWriter writer = new PdfLayoutWriter(document)) {
                writer.writeHero(project, version, priorityLinks.size(), sortedDataLinks.size(), generatedAt);
                writer.writeSection("Resume");
                writer.writeMetricGrid(metrics);

                if (sortedDataLinks.isEmpty()) {
                    writer.writeSection("Data links");
                    writer.writeEmptyState("Aucun data link disponible pour cette version.");
                } else {
                    writer.writeSection("Data links prioritaires");
                    if (priorityLinks.isEmpty()) {
                        writer.writeEmptyState("Aucun data link prioritaire detecte.");
                    } else {
                        for (int index = 0; index < priorityLinks.size(); index++) {
                            writer.writeDataLinkCard(index + 1, priorityLinks.get(index), true);
                        }
                    }

                    if (!secondaryLinks.isEmpty()) {
                        writer.writeSection("Autres data links");
                        for (int index = 0; index < secondaryLinks.size(); index++) {
                            writer.writeDataLinkCard(index + 1, secondaryLinks.get(index), false);
                        }
                    }
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate version PDF export", e);
        }
    }

    private static boolean isPriorityLink(DataLinkResponse dataLink) {
        return dataLink.match() == MatchType.MISSING
            || dataLink.severity() == Severity.CRITICAL
            || dataLink.severity() == Severity.MAJOR;
    }

    private static int priorityBucket(DataLinkResponse dataLink) {
        return isPriorityLink(dataLink) ? 1 : 0;
    }

    private static int severityRank(DataLinkResponse dataLink) {
        if (dataLink.severity() == null) {
            return 0;
        }

        return switch (dataLink.severity()) {
            case MINOR -> 1;
            case MAJOR -> 2;
            case CRITICAL -> 3;
        };
    }

    private static int matchRank(DataLinkResponse dataLink) {
        return switch (dataLink.match()) {
            case EXACT -> 1;
            case SEMANTIC -> 2;
            case MISSING -> 3;
        };
    }

    private static List<DataLinkResponse> safeDataLinks(VersionDetailsResponse version) {
        return version.getDataLinks() == null ? List.of() : version.getDataLinks();
    }

    private long countByMatch(List<DataLinkResponse> dataLinks, MatchType matchType) {
        return dataLinks.stream()
            .map(DataLinkResponse::match)
            .filter(matchType::equals)
            .count();
    }

    private long countBySeverity(List<DataLinkResponse> dataLinks, Severity severity) {
        return dataLinks.stream()
            .map(DataLinkResponse::severity)
            .filter(severity::equals)
            .count();
    }

    private String formatPreview(ParsedDataResponse parsedData) {
        if (parsedData == null) {
            return "n/a";
        }

        return abbreviate(parsedData.content(), MAX_PREVIEW_LENGTH);
    }

    private String formatPanelMeta(ParsedDataResponse parsedData) {
        if (parsedData == null) {
            return "n/a";
        }

        return formatValue(parsedData.fileType()) + " / " + formatValue(parsedData.dataType());
    }

    private String formatPanelFileName(ParsedDataResponse parsedData) {
        if (parsedData == null) {
            return "n/a";
        }

        return sanitizeText(parsedData.fileName());
    }

    private String formatConfidence(Integer confidenceScore) {
        return confidenceScore == null ? "n/a" : confidenceScore + "/100";
    }

    private boolean hasMeaningfulText(String text) {
        return !"n/a".equalsIgnoreCase(sanitizeText(text));
    }

    private String formatValue(Object value) {
        return value == null ? "n/a" : sanitizeText(String.valueOf(value));
    }

    private String abbreviate(String text, int maxLength) {
        String sanitized = sanitizeText(text);
        if (sanitized.length() <= maxLength) {
            return sanitized;
        }
        return sanitized.substring(0, maxLength - 3) + "...";
    }

    private String formatTimestamp(Instant timestamp) {
        return PDF_TIMESTAMP_FORMAT.format(timestamp);
    }

    private String sanitizeText(String text) {
        if (text == null || text.isBlank()) {
            return "n/a";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replaceAll("[\\r\\n\\t]+", " ")
            .replaceAll("\\s{2,}", " ")
            .trim();

        StringBuilder asciiText = new StringBuilder(normalized.length());
        for (char character : normalized.toCharArray()) {
            if (character >= 32 && character <= 126) {
                asciiText.append(character);
            } else {
                asciiText.append('?');
            }
        }
        return asciiText.toString();
    }

    private String renderableText(String text) {
        if (text == null) {
            return "";
        }

        if (text.isEmpty() || text.isBlank()) {
            return text;
        }

        return sanitizeText(text);
    }

    private Color matchColor(MatchType matchType) {
        return switch (matchType) {
            case EXACT -> EXACT_COLOR;
            case SEMANTIC -> SEMANTIC_COLOR;
            case MISSING -> MISSING_COLOR;
        };
    }

    private Color severityColor(Severity severity) {
        if (severity == null) {
            return MINOR_COLOR;
        }

        return switch (severity) {
            case MINOR -> MINOR_COLOR;
            case MAJOR -> MAJOR_COLOR;
            case CRITICAL -> CRITICAL_COLOR;
        };
    }

    private Color tint(Color color, float ratio) {
        ratio = Math.max(0f, Math.min(ratio, 1f));
        int red = (int) (color.getRed() + ((255 - color.getRed()) * ratio));
        int green = (int) (color.getGreen() + ((255 - color.getGreen()) * ratio));
        int blue = (int) (color.getBlue() + ((255 - color.getBlue()) * ratio));
        return new Color(red, green, blue);
    }

    private record MetricCard(String label, String value, Color accent) {
    }

    private final class PdfLayoutWriter implements AutoCloseable {

        private final PDDocument document;

        private PDPage page;
        private PDPageContentStream contentStream;
        private float currentY;
        private int pageNumber;

        private PdfLayoutWriter(PDDocument document) throws IOException {
            this.document = document;
            startNewPage();
        }

        private void writeHero(
            ProjectEntity project,
            VersionDetailsResponse version,
            int priorityCount,
            int totalLinks,
            Instant generatedAt
        )
            throws IOException {
            float heroHeight = 104f;
            ensureBlockSpace(heroHeight + 10f);

            float top = currentY;
            float bottom = top - heroHeight;
            fillRect(PAGE_MARGIN, bottom, WRITABLE_WIDTH, heroHeight, HERO_BACKGROUND);
            fillRect(PAGE_MARGIN, bottom, WRITABLE_WIDTH, 5f, HERO_ACCENT);

            float textX = PAGE_MARGIN + 18f;
            float y = top - 27f;
            drawSingleLine("Rapport Corekt", Standard14Fonts.FontName.HELVETICA_BOLD, 20f, HERO_TEXT, textX, y);
            y -= 22f;
            drawSingleLine(
                sanitizeText(project.getTitle()) + "  /  " + sanitizeText(version.getTitle()) + " (#" + version.getVersion() + ")",
                Standard14Fonts.FontName.HELVETICA,
                12f,
                HERO_MUTED,
                textX,
                y
            );
            y -= 18f;
            drawSingleLine(
                "Export du " + formatTimestamp(generatedAt),
                Standard14Fonts.FontName.HELVETICA,
                10f,
                HERO_MUTED,
                textX,
                y
            );

            y -= 22f;
            float chipX = textX;
            chipX = drawChip(chipX, y, "Statut: " + formatValue(version.getStatus()), SECTION_COLOR, HERO_TEXT) + CHIP_GAP;
            chipX = drawChip(chipX, y, "Liens: " + totalLinks, tint(HERO_ACCENT, 0.18f), HERO_TEXT) + CHIP_GAP;
            drawChip(chipX, y, "Prioritaires: " + priorityCount, tint(CRITICAL_COLOR, 0.08f), HERO_TEXT);

            currentY = bottom - 14f;
        }

        private void writeSection(String text) throws IOException {
            currentY -= SECTION_SPACING;
            ensureBlockSpace(28f);

            drawSingleLine(text, Standard14Fonts.FontName.HELVETICA_BOLD, 14f, SECTION_COLOR, PAGE_MARGIN, currentY);
            float lineX = PAGE_MARGIN + textWidth(text, font(Standard14Fonts.FontName.HELVETICA_BOLD), 14f) + 12f;
            drawLine(lineX, currentY + 3f, PAGE_MARGIN + WRITABLE_WIDTH, currentY + 3f, BORDER_COLOR, 1f);
            currentY -= 14f;
        }

        private void writeMetricGrid(List<MetricCard> metrics) throws IOException {
            for (int index = 0; index < metrics.size(); index += 3) {
                ensureBlockSpace(METRIC_CARD_HEIGHT + METRIC_CARD_GAP);
                float rowTop = currentY;
                int rowCount = Math.min(3, metrics.size() - index);
                float rowWidth = (rowCount * METRIC_CARD_WIDTH) + ((rowCount - 1) * METRIC_CARD_GAP);
                float startX = PAGE_MARGIN + ((WRITABLE_WIDTH - rowWidth) / 2f);

                for (int column = 0; column < rowCount; column++) {
                    MetricCard card = metrics.get(index + column);
                    float x = startX + (column * (METRIC_CARD_WIDTH + METRIC_CARD_GAP));
                    drawMetricCard(card, x, rowTop);
                }

                currentY = rowTop - METRIC_CARD_HEIGHT - METRIC_CARD_GAP;
            }
        }

        private void drawMetricCard(MetricCard metric, float x, float top) throws IOException {
            float bottom = top - METRIC_CARD_HEIGHT;
            fillRect(x, bottom, METRIC_CARD_WIDTH, METRIC_CARD_HEIGHT, Color.WHITE);
            strokeRect(x, bottom, METRIC_CARD_WIDTH, METRIC_CARD_HEIGHT, BORDER_COLOR, 0.8f);
            fillRect(x, bottom, 5f, METRIC_CARD_HEIGHT, metric.accent());

            drawSingleLine(metric.value(), Standard14Fonts.FontName.HELVETICA_BOLD, 21f, TEXT_PRIMARY, x + 18f, top - 24f);
            drawWrappedLines(
                metric.label(),
                Standard14Fonts.FontName.HELVETICA,
                9.5f,
                TEXT_SECONDARY,
                x + 18f,
                top - 44f,
                METRIC_CARD_WIDTH - 34f,
                11.5f
            );
        }

        private void writeEmptyState(String text) throws IOException {
            float boxHeight = 54f;
            ensureBlockSpace(boxHeight + BLOCK_SPACING);

            float top = currentY;
            float bottom = top - boxHeight;
            fillRect(PAGE_MARGIN, bottom, WRITABLE_WIDTH, boxHeight, MUTED_BACKGROUND);
            strokeRect(PAGE_MARGIN, bottom, WRITABLE_WIDTH, boxHeight, BORDER_COLOR, 0.8f);
            drawWrappedLines(
                text,
                Standard14Fonts.FontName.HELVETICA,
                10.5f,
                TEXT_SECONDARY,
                PAGE_MARGIN + 16f,
                top - 21f,
                WRITABLE_WIDTH - 32f,
                14f
            );

            currentY = bottom - BLOCK_SPACING;
        }

        private void writeDataLinkCard(int index, DataLinkResponse dataLink, boolean priority) throws IOException {
            Color matchColor = matchColor(dataLink.match());
            Color severityColor = severityColor(dataLink.severity());
            Color accentColor = priority ? severityColor : matchColor;
            Color backgroundColor = priority ? tint(accentColor, 0.93f) : CARD_BACKGROUND;

            float contentWidth = WRITABLE_WIDTH - DATA_LINK_ACCENT_WIDTH - (DATA_LINK_PADDING * 2);
            float detailPanelWidth = (contentWidth - DETAIL_PANEL_GAP) / 2f;
            FontStyle titleStyle = new FontStyle(Standard14Fonts.FontName.HELVETICA_BOLD, 12f, TEXT_PRIMARY, 15f);
            FontStyle fileStyle = new FontStyle(Standard14Fonts.FontName.HELVETICA_BOLD, 10.5f, TEXT_PRIMARY, 13.5f);
            FontStyle metaStyle = new FontStyle(Standard14Fonts.FontName.HELVETICA_BOLD, 8.5f, TEXT_SECONDARY, 11f);
            FontStyle mutedStyle = new FontStyle(Standard14Fonts.FontName.HELVETICA, 9.2f, TEXT_SECONDARY, 12.2f);
            FontStyle emphasisStyle = new FontStyle(Standard14Fonts.FontName.HELVETICA_OBLIQUE, 9.2f, TEXT_SECONDARY, 12.2f);
            FontStyle panelTitleStyle = new FontStyle(Standard14Fonts.FontName.HELVETICA_BOLD, 9f, SECTION_COLOR, 12f);
            FontStyle previewLabelStyle = new FontStyle(Standard14Fonts.FontName.HELVETICA_BOLD, 8.2f, TEXT_SECONDARY, 10f);
            FontStyle justificationHighlightStyle = new FontStyle(Standard14Fonts.FontName.HELVETICA_BOLD, 9.2f, accentColor, 12.2f);

            List<String> titleLines = wrapText("Data link #" + index, titleStyle.font(), titleStyle.fontSize(), contentWidth);
            List<String> sourceMetaLines = wrapText(
                formatPanelMeta(dataLink.sourceParsedData()),
                metaStyle.font(),
                metaStyle.fontSize(),
                detailPanelWidth - (DETAIL_PANEL_PADDING * 2)
            );
            List<String> sourceFileLines = wrapText(
                formatPanelFileName(dataLink.sourceParsedData()),
                fileStyle.font(),
                fileStyle.fontSize(),
                detailPanelWidth - (DETAIL_PANEL_PADDING * 2)
            );
            String sourcePreview = formatPreview(dataLink.sourceParsedData());
            List<String> sourcePreviewLines = wrapText(
                sourcePreview,
                emphasisStyle.font(),
                emphasisStyle.fontSize(),
                detailPanelWidth - (DETAIL_PANEL_PADDING * 2) - (PREVIEW_BOX_PADDING * 2)
            );
            List<String> targetMetaLines = wrapText(
                formatPanelMeta(dataLink.targetParsedData()),
                metaStyle.font(),
                metaStyle.fontSize(),
                detailPanelWidth - (DETAIL_PANEL_PADDING * 2)
            );
            List<String> targetFileLines = wrapText(
                formatPanelFileName(dataLink.targetParsedData()),
                fileStyle.font(),
                fileStyle.fontSize(),
                detailPanelWidth - (DETAIL_PANEL_PADDING * 2)
            );
            String targetPreview = formatPreview(dataLink.targetParsedData());
            List<String> targetPreviewLines = wrapText(
                targetPreview,
                emphasisStyle.font(),
                emphasisStyle.fontSize(),
                detailPanelWidth - (DETAIL_PANEL_PADDING * 2) - (PREVIEW_BOX_PADDING * 2)
            );
            String justificationText = abbreviate(dataLink.justification(), 520);
            String suggestionText = abbreviate(dataLink.suggestion(), 320);
            List<String> justificationLines = wrapText(
                justificationText,
                mutedStyle.font(),
                mutedStyle.fontSize(),
                contentWidth - 24f
            );
            boolean hasSuggestion = hasMeaningfulText(suggestionText);
            List<String> suggestionLines = hasSuggestion
                ? wrapText(suggestionText, mutedStyle.font(), mutedStyle.fontSize(), contentWidth - 24f)
                : List.of();

            float sourcePanelHeight = measureInfoPanelHeight(
                sourceMetaLines,
                sourceFileLines,
                hasMeaningfulText(sourcePreview) ? sourcePreviewLines : List.of(),
                metaStyle,
                fileStyle,
                emphasisStyle,
                panelTitleStyle,
                previewLabelStyle
            );
            float targetPanelHeight = measureInfoPanelHeight(
                targetMetaLines,
                targetFileLines,
                hasMeaningfulText(targetPreview) ? targetPreviewLines : List.of(),
                metaStyle,
                fileStyle,
                emphasisStyle,
                panelTitleStyle,
                previewLabelStyle
            );
            float infoPanelsHeight = Math.max(sourcePanelHeight, targetPanelHeight);
            float justificationHeight = measureHighlightedNoteBlockHeight(
                justificationText,
                mutedStyle,
                justificationHighlightStyle,
                panelTitleStyle,
                contentWidth
            );
            float suggestionHeight = hasSuggestion ? measureNoteBlockHeight(suggestionLines, mutedStyle, panelTitleStyle) : 0f;

            float cardHeight = 18f
                + (titleLines.size() * titleStyle.lineHeight())
                + 24f
                + infoPanelsHeight
                + 8f
                + justificationHeight
                + (hasSuggestion ? 8f + suggestionHeight : 0f)
                + 16f;

            ensureBlockSpace(cardHeight + BLOCK_SPACING);

            float top = currentY;
            float bottom = top - cardHeight;
            fillRect(PAGE_MARGIN, bottom, WRITABLE_WIDTH, cardHeight, backgroundColor);
            strokeRect(PAGE_MARGIN, bottom, WRITABLE_WIDTH, cardHeight, BORDER_COLOR, 0.8f);
            fillRect(PAGE_MARGIN, bottom, DATA_LINK_ACCENT_WIDTH, cardHeight, accentColor);

            float x = PAGE_MARGIN + DATA_LINK_ACCENT_WIDTH + DATA_LINK_PADDING;
            float y = top - 16f;
            y = drawTextLines(titleLines, titleStyle, x, y, contentWidth);

            float chipTop = y + 4f;
            float chipX = x;
            chipX = drawChip(chipX, chipTop, formatValue(dataLink.match()), tint(matchColor, 0.2f), TEXT_PRIMARY) + CHIP_GAP;
            chipX = drawChip(chipX, chipTop, formatValue(dataLink.severity()), tint(severityColor, 0.2f), TEXT_PRIMARY) + CHIP_GAP;
            drawChip(chipX, chipTop, "Confiance " + formatConfidence(dataLink.confidenceScore()), MUTED_BACKGROUND, TEXT_SECONDARY);
            y -= 24f;

            drawInfoPanel(
                x,
                y,
                detailPanelWidth,
                sourcePanelHeight,
                "Source",
                sourceMetaLines,
                sourceFileLines,
                hasMeaningfulText(sourcePreview) ? sourcePreviewLines : List.of(),
                metaStyle,
                fileStyle,
                emphasisStyle,
                panelTitleStyle,
                previewLabelStyle,
                tint(matchColor, 0.9f)
            );
            drawInfoPanel(
                x + detailPanelWidth + DETAIL_PANEL_GAP,
                y,
                detailPanelWidth,
                targetPanelHeight,
                "Cible",
                targetMetaLines,
                targetFileLines,
                hasMeaningfulText(targetPreview) ? targetPreviewLines : List.of(),
                metaStyle,
                fileStyle,
                emphasisStyle,
                panelTitleStyle,
                previewLabelStyle,
                tint(accentColor, 0.9f)
            );

            y -= infoPanelsHeight + 8f;
            y = drawHighlightedNoteBlock(
                x,
                y,
                contentWidth,
                "Justification",
                justificationText,
                mutedStyle,
                justificationHighlightStyle,
                panelTitleStyle,
                MUTED_BACKGROUND
            );
            if (hasSuggestion) {
                y -= 8f;
                y = drawNoteBlock(x, y, contentWidth, "Suggestion", suggestionLines, mutedStyle, panelTitleStyle, tint(accentColor, 0.92f));
            }

            currentY = bottom - BLOCK_SPACING;
        }

        private float measureInfoPanelHeight(
            List<String> metaLines,
            List<String> fileLines,
            List<String> previewLines,
            FontStyle metaStyle,
            FontStyle fileStyle,
            FontStyle previewStyle,
            FontStyle titleStyle,
            FontStyle previewLabelStyle
        ) {
            float height = 12f
                + titleStyle.lineHeight()
                + 8f
                + (metaLines.size() * metaStyle.lineHeight())
                + 4f
                + (fileLines.size() * fileStyle.lineHeight())
                + 12f;

            if (!previewLines.isEmpty()) {
                height += measurePreviewBoxHeight(previewLines, previewStyle, previewLabelStyle) + 10f;
            }

            return height;
        }

        private void drawInfoPanel(
            float x,
            float top,
            float width,
            float measuredHeight,
            String title,
            List<String> metaLines,
            List<String> fileLines,
            List<String> previewLines,
            FontStyle metaStyle,
            FontStyle fileStyle,
            FontStyle previewStyle,
            FontStyle titleStyle,
            FontStyle previewLabelStyle,
            Color previewBackground
        ) throws IOException {
            float bottom = top - measuredHeight;
            fillRect(x, bottom, width, measuredHeight, Color.WHITE);
            strokeRect(x, bottom, width, measuredHeight, BORDER_COLOR, 0.8f);

            float y = top - 14f;
            drawSingleLine(title, titleStyle.fontName(), titleStyle.fontSize(), titleStyle.color(), x + DETAIL_PANEL_PADDING, y);
            y -= titleStyle.lineHeight() + 4f;
            drawLine(x + DETAIL_PANEL_PADDING, y + 3f, x + width - DETAIL_PANEL_PADDING, y + 3f, BORDER_COLOR, 0.7f);
            y -= 8f;
            y = drawTextLines(metaLines, metaStyle, x + DETAIL_PANEL_PADDING, y, width - (DETAIL_PANEL_PADDING * 2));
            y -= 2f;
            y = drawTextLines(fileLines, fileStyle, x + DETAIL_PANEL_PADDING, y, width - (DETAIL_PANEL_PADDING * 2));

            if (!previewLines.isEmpty()) {
                y -= 8f;
                drawPreviewBox(
                    x + DETAIL_PANEL_PADDING,
                    y,
                    width - (DETAIL_PANEL_PADDING * 2),
                    previewLines,
                    previewStyle,
                    previewLabelStyle,
                    previewBackground
                );
            }
        }

        private float measurePreviewBoxHeight(
            List<String> previewLines,
            FontStyle previewStyle,
            FontStyle previewLabelStyle
        ) {
            return PREVIEW_BOX_PADDING
                + previewLabelStyle.lineHeight()
                + 5f
                + (previewLines.size() * previewStyle.lineHeight())
                + PREVIEW_BOX_PADDING;
        }

        private float drawPreviewBox(
            float x,
            float top,
            float width,
            List<String> previewLines,
            FontStyle previewStyle,
            FontStyle previewLabelStyle,
            Color background
        ) throws IOException {
            float height = measurePreviewBoxHeight(previewLines, previewStyle, previewLabelStyle);
            float bottom = top - height;

            fillRect(x, bottom, width, height, background);
            fillRect(x, bottom, 3f, height, tint(SECTION_COLOR, 0.45f));

            float y = top - PREVIEW_BOX_PADDING;
            drawSingleLine("Extrait", previewLabelStyle.fontName(), previewLabelStyle.fontSize(), previewLabelStyle.color(), x + PREVIEW_BOX_PADDING + 3f, y);
            y -= previewLabelStyle.lineHeight() + 5f;
            drawTextLines(
                previewLines,
                previewStyle,
                x + PREVIEW_BOX_PADDING + 3f,
                y,
                width - ((PREVIEW_BOX_PADDING + 3f) * 2)
            );
            return bottom;
        }

        private float measureNoteBlockHeight(List<String> lines, FontStyle bodyStyle, FontStyle titleStyle) {
            return 12f
                + titleStyle.lineHeight()
                + 8f
                + (lines.size() * bodyStyle.lineHeight())
                + 12f;
        }

        private float measureHighlightedNoteBlockHeight(
            String text,
            FontStyle bodyStyle,
            FontStyle highlightStyle,
            FontStyle titleStyle,
            float width
        ) throws IOException {
            List<List<InlineFragment>> lines = wrapAnnotatedText(text, bodyStyle, highlightStyle, width - 28f);
            return 12f
                + titleStyle.lineHeight()
                + 8f
                + (lines.size() * bodyStyle.lineHeight())
                + 12f;
        }

        private float drawNoteBlock(
            float x,
            float top,
            float width,
            String title,
            List<String> lines,
            FontStyle bodyStyle,
            FontStyle titleStyle,
            Color background
        ) throws IOException {
            float height = measureNoteBlockHeight(lines, bodyStyle, titleStyle);
            float bottom = top - height;

            fillRect(x, bottom, width, height, background);
            strokeRect(x, bottom, width, height, BORDER_COLOR, 0.8f);
            fillRect(x, bottom, 4f, height, tint(titleStyle.color(), 0.55f));

            float y = top - 14f;
            drawSingleLine(title, titleStyle.fontName(), titleStyle.fontSize(), titleStyle.color(), x + 14f, y);
            y -= titleStyle.lineHeight() + 4f;
            drawTextLines(lines, bodyStyle, x + 14f, y, width - 28f);
            return bottom;
        }

        private float drawHighlightedNoteBlock(
            float x,
            float top,
            float width,
            String title,
            String text,
            FontStyle bodyStyle,
            FontStyle highlightStyle,
            FontStyle titleStyle,
            Color background
        ) throws IOException {
            List<List<InlineFragment>> lines = wrapAnnotatedText(text, bodyStyle, highlightStyle, width - 28f);
            float height = 12f
                + titleStyle.lineHeight()
                + 8f
                + (lines.size() * bodyStyle.lineHeight())
                + 12f;
            float bottom = top - height;

            fillRect(x, bottom, width, height, background);
            strokeRect(x, bottom, width, height, BORDER_COLOR, 0.8f);
            fillRect(x, bottom, 4f, height, tint(titleStyle.color(), 0.55f));

            float y = top - 14f;
            drawSingleLine(title, titleStyle.fontName(), titleStyle.fontSize(), titleStyle.color(), x + 14f, y);
            y -= titleStyle.lineHeight() + 4f;
            drawStyledLines(lines, x + 14f, y, bodyStyle.lineHeight());
            return bottom;
        }

        private float drawChip(float x, float top, String text, Color background, Color textColor) throws IOException {
            String sanitized = sanitizeText(text);
            PDType1Font font = font(Standard14Fonts.FontName.HELVETICA_BOLD);
            float fontSize = 8.5f;
            float textWidth = textWidth(sanitized, font, fontSize);
            float width = textWidth + 12f;
            float bottom = top - CHIP_HEIGHT;

            fillRect(x, bottom, width, CHIP_HEIGHT, background);
            strokeRect(x, bottom, width, CHIP_HEIGHT, tint(background, 0.18f), 0.6f);
            drawSingleLine(sanitized, Standard14Fonts.FontName.HELVETICA_BOLD, fontSize, textColor, x + 6f, top - 12f);
            return x + width;
        }

        private float drawTextLines(List<String> lines, FontStyle style, float x, float y, float maxWidth) throws IOException {
            for (String line : lines) {
                drawSingleLine(line, style.fontName(), style.fontSize(), style.color(), x, y);
                y -= style.lineHeight();
            }
            return y;
        }

        private float drawStyledLines(List<List<InlineFragment>> lines, float x, float y, float lineHeight) throws IOException {
            for (List<InlineFragment> line : lines) {
                float currentX = x;
                for (InlineFragment fragment : line) {
                    if (!fragment.text().isBlank()) {
                        drawSingleLine(
                            fragment.text(),
                            fragment.style().fontName(),
                            fragment.style().fontSize(),
                            fragment.style().color(),
                            currentX,
                            y
                        );
                    }
                    float fragmentWidth = textWidth(fragment.text(), fragment.style().font(), fragment.style().fontSize());
                    if (fragment.underline() && !fragment.text().isBlank()) {
                        float underlineY = y - 1.8f;
                        drawLine(currentX, underlineY, currentX + fragmentWidth, underlineY, tint(fragment.style().color(), 0.1f), 0.75f);
                    }
                    currentX += fragmentWidth;
                }
                y -= lineHeight;
            }
            return y;
        }

        private void drawWrappedLines(
            String text,
            Standard14Fonts.FontName fontName,
            float fontSize,
            Color color,
            float x,
            float y,
            float width,
            float lineHeight
        ) throws IOException {
            FontStyle style = new FontStyle(fontName, fontSize, color, lineHeight);
            drawTextLines(wrapText(text, style.font(), style.fontSize(), width), style, x, y, width);
        }

        private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder currentLine = new StringBuilder();
            String sanitized = sanitizeText(text);

            for (String word : sanitized.split(" ")) {
                if (word.isBlank()) {
                    continue;
                }

                String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (textWidth(candidate, font, fontSize) <= maxWidth) {
                    currentLine.setLength(0);
                    currentLine.append(candidate);
                    continue;
                }

                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                }

                if (textWidth(word, font, fontSize) <= maxWidth) {
                    currentLine.append(word);
                    continue;
                }

                lines.addAll(splitLongWord(word, font, fontSize, maxWidth));
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }

            if (lines.isEmpty()) {
                lines.add("n/a");
            }
            return lines;
        }

        private List<String> splitLongWord(String word, PDType1Font font, float fontSize, float maxWidth) throws IOException {
            List<String> chunks = new ArrayList<>();
            StringBuilder chunk = new StringBuilder();

            for (char character : word.toCharArray()) {
                String candidate = chunk.toString() + character;
                if (textWidth(candidate, font, fontSize) <= maxWidth || chunk.isEmpty()) {
                    chunk.append(character);
                    continue;
                }

                chunks.add(chunk.toString());
                chunk.setLength(0);
                chunk.append(character);
            }

            if (!chunk.isEmpty()) {
                chunks.add(chunk.toString());
            }

            return chunks;
        }

        private List<List<InlineFragment>> wrapAnnotatedText(
            String text,
            FontStyle defaultStyle,
            FontStyle highlightStyle,
            float maxWidth
        ) throws IOException {
            List<InlineFragment> tokens = tokenizeAnnotatedText(text, defaultStyle, highlightStyle);
            List<List<InlineFragment>> lines = new ArrayList<>();
            List<InlineFragment> currentLine = new ArrayList<>();
            float currentWidth = 0f;

            for (InlineFragment token : tokens) {
                float tokenWidth = textWidth(token.text(), token.style().font(), token.style().fontSize());
                boolean whitespace = token.text().isBlank();

                if (whitespace && currentLine.isEmpty()) {
                    continue;
                }

                if (!whitespace && tokenWidth > maxWidth) {
                    if (!currentLine.isEmpty()) {
                        trimTrailingSpaces(currentLine);
                        lines.add(List.copyOf(currentLine));
                        currentLine.clear();
                        currentWidth = 0f;
                    }

                    for (String chunk : splitLongWord(token.text(), token.style().font(), token.style().fontSize(), maxWidth)) {
                        currentLine.add(new InlineFragment(chunk, token.style(), token.underline()));
                        trimTrailingSpaces(currentLine);
                        lines.add(List.copyOf(currentLine));
                        currentLine.clear();
                    }
                    continue;
                }

                if (currentWidth + tokenWidth > maxWidth && !currentLine.isEmpty()) {
                    trimTrailingSpaces(currentLine);
                    lines.add(List.copyOf(currentLine));
                    currentLine.clear();
                    currentWidth = 0f;

                    if (whitespace) {
                        continue;
                    }
                }

                currentLine.add(token);
                currentWidth += tokenWidth;
            }

            trimTrailingSpaces(currentLine);
            if (!currentLine.isEmpty()) {
                lines.add(List.copyOf(currentLine));
            }

            if (lines.isEmpty()) {
                lines.add(List.of(new InlineFragment("n/a", defaultStyle, false)));
            }

            return lines;
        }

        private List<InlineFragment> tokenizeAnnotatedText(
            String text,
            FontStyle defaultStyle,
            FontStyle highlightStyle
        ) {
            List<InlineFragment> tokens = new ArrayList<>();
            String sanitized = sanitizeText(text);
            Matcher matcher = JUSTIFICATION_HIGHLIGHT_PATTERN.matcher(sanitized);
            int cursor = 0;

            while (matcher.find()) {
                if (matcher.start() > cursor) {
                    addSegmentTokens(tokens, sanitized.substring(cursor, matcher.start()), defaultStyle, false);
                }
                addSegmentTokens(tokens, matcher.group(), highlightStyle, true);
                cursor = matcher.end();
            }

            if (cursor < sanitized.length()) {
                addSegmentTokens(tokens, sanitized.substring(cursor), defaultStyle, false);
            }

            return tokens;
        }

        private void addSegmentTokens(
            List<InlineFragment> tokens,
            String segment,
            FontStyle style,
            boolean underline
        ) {
            Matcher matcher = Pattern.compile("\\S+|\\s+").matcher(segment);
            while (matcher.find()) {
                tokens.add(new InlineFragment(matcher.group(), style, underline));
            }
        }

        private void trimTrailingSpaces(List<InlineFragment> fragments) {
            while (!fragments.isEmpty() && fragments.get(fragments.size() - 1).text().isBlank()) {
                fragments.remove(fragments.size() - 1);
            }
        }

        private void ensureBlockSpace(float blockHeight) throws IOException {
            if (currentY - blockHeight <= MIN_BOTTOM_MARGIN) {
                startNewPage();
            }
        }

        private void startNewPage() throws IOException {
            closeContentStream();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            pageNumber++;
            contentStream = new PDPageContentStream(document, page);
            currentY = PAGE_HEIGHT - PAGE_MARGIN;

            fillRect(0f, 0f, PAGE_WIDTH, PAGE_HEIGHT, PAGE_BACKGROUND);
            drawLine(PAGE_MARGIN, PAGE_HEIGHT - 20f, PAGE_MARGIN + WRITABLE_WIDTH, PAGE_HEIGHT - 20f, BORDER_COLOR, 0.8f);
            drawLine(PAGE_MARGIN, 26f, PAGE_MARGIN + WRITABLE_WIDTH, 26f, BORDER_COLOR, 0.8f);
            drawSingleLine("Corekt PDF export", Standard14Fonts.FontName.HELVETICA, 8.5f, TEXT_SECONDARY, PAGE_MARGIN, 18f);
            drawSingleLine("Page " + pageNumber, Standard14Fonts.FontName.HELVETICA, 8.5f, TEXT_SECONDARY, PAGE_MARGIN + WRITABLE_WIDTH - 34f, 18f);
        }

        private void drawSingleLine(
            String text,
            Standard14Fonts.FontName fontName,
            float fontSize,
            Color color,
            float x,
            float y
        ) throws IOException {
            contentStream.beginText();
            contentStream.setFont(font(fontName), fontSize);
            contentStream.setNonStrokingColor(color);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(renderableText(text));
            contentStream.endText();
        }

        private void fillRect(float x, float y, float width, float height, Color color) throws IOException {
            contentStream.setNonStrokingColor(color);
            contentStream.addRect(x, y, width, height);
            contentStream.fill();
        }

        private void strokeRect(float x, float y, float width, float height, Color color, float lineWidth) throws IOException {
            contentStream.setStrokingColor(color);
            contentStream.setLineWidth(lineWidth);
            contentStream.addRect(x, y, width, height);
            contentStream.stroke();
        }

        private void drawLine(float startX, float startY, float endX, float endY, Color color, float lineWidth)
            throws IOException {
            contentStream.setStrokingColor(color);
            contentStream.setLineWidth(lineWidth);
            contentStream.moveTo(startX, startY);
            contentStream.lineTo(endX, endY);
            contentStream.stroke();
        }

        private float textWidth(String text, PDType1Font font, float fontSize) throws IOException {
            return font.getStringWidth(renderableText(text)) / 1000f * fontSize;
        }

        private PDType1Font font(Standard14Fonts.FontName fontName) {
            return new PDType1Font(fontName);
        }

        private void closeContentStream() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        @Override
        public void close() throws IOException {
            closeContentStream();
        }
    }

    private record FontStyle(
        Standard14Fonts.FontName fontName,
        float fontSize,
        Color color,
        float lineHeight
    ) {
        private PDType1Font font() {
            return new PDType1Font(fontName);
        }
    }

    private record InlineFragment(
        String text,
        FontStyle style,
        boolean underline
    ) {
    }
}
