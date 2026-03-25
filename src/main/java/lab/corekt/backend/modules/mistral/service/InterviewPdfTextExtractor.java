package lab.corekt.backend.modules.mistral.service;

import lab.corekt.backend.modules.upload.entity.FileEntity;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class InterviewPdfTextExtractor {

    public String extractText(FileEntity interviewFile) {
        if (interviewFile == null) {
            throw new IllegalArgumentException("Interview file must not be null");
        }
        return extractText(interviewFile.getFile());
    }

    public String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF content must not be empty");
        }

        try (PDDocument document = Loader.loadPDF(new ByteArrayInputStream(pdfBytes).readAllBytes())) {
            String text = new PDFTextStripper().getText(document);

            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Could not extract text from PDF");
            }

            return text;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract text from PDF", e);
        }
    }
}
