package lab.corekt.backend.modules.mistral.service;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewPdfTextExtractorTest {

    private final InterviewPdfTextExtractor extractor = new InterviewPdfTextExtractor();

    @Test
    @DisplayName("Should extract text from PDF bytes")
    void testExtractText() throws IOException {
        byte[] pdfBytes = readTestPdf();

        String text = extractor.extractText(pdfBytes);

        assertThat(text).isNotBlank();
    }

    @Test
    @DisplayName("Should extract text from interview FileEntity")
    void testExtractTextFromFileEntity() throws IOException {
        FileEntity interviewFile = new FileEntity(FileType.INTERVIEW, "files/interview.pdf");
        interviewFile.setFile(readTestPdf());

        String text = extractor.extractText(interviewFile);

        assertThat(text).isNotBlank();
    }

    @Test
    @DisplayName("Should reject empty PDF content")
    void testExtractTextWithEmptyBytes() {
        assertThatThrownBy(() -> extractor.extractText(new byte[0]))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PDF content must not be empty");
    }

    @Test
    @DisplayName("Should reject null FileEntity")
    void testExtractTextWithNullFileEntity() {
        assertThatThrownBy(() -> extractor.extractText((FileEntity) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Interview file must not be null");
    }

    private byte[] readTestPdf() throws IOException {
        Path path = Path.of("src/test/resources/files/interview.pdf");
        return Files.readAllBytes(path);
    }
}
