package lab.corekt.backend.modules.project.parsing.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileType Enum Tests")
class FileTypeTest {

    @Nested
    @DisplayName("BPMN File Type Tests")
    class BpmnFileTypeTests {

        @Test
        @DisplayName("isValidFile accepts .bpmn extension")
        void acceptsBpmnExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
            );

            assertTrue(FileType.BPMN.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile accepts .BPMN uppercase extension")
        void acceptsBpmnUppercaseExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diagram.BPMN", "application/xml", "<bpmn />".getBytes()
            );

            assertTrue(FileType.BPMN.isValidFile(file));
        }

        @ParameterizedTest
        @ValueSource(strings = {"diagram.xml", "diagram.txt", "diagram.pdf", "diagram.mcd"})
        @DisplayName("isValidFile rejects non-bpmn extensions")
        void rejectsNonBpmnExtension(String filename) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "application/xml", "content".getBytes()
            );

            assertFalse(FileType.BPMN.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile rejects file without extension")
        void rejectsFileWithoutExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diagram", "application/xml", "<bpmn />".getBytes()
            );

            assertFalse(FileType.BPMN.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile rejects null filename")
        void rejectsNullFilename() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", null, "application/xml", "<bpmn />".getBytes()
            );

            assertFalse(FileType.BPMN.isValidFile(file));
        }
    }

    @Nested
    @DisplayName("PDF Interview File Type Tests")
    class InterviewFileTypeTests {

        @Test
        @DisplayName("isValidFile accepts .pdf extension")
        void acceptsPdfExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "interview.pdf", "application/pdf", "pdf content".getBytes()
            );

            assertTrue(FileType.INTERVIEW.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile accepts .PDF uppercase extension")
        void acceptsPdfUppercaseExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "interview.PDF", "application/pdf", "pdf content".getBytes()
            );

            assertTrue(FileType.INTERVIEW.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile accepts .Pdf mixed case extension")
        void acceptsPdfMixedCaseExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "interview.Pdf", "application/pdf", "pdf content".getBytes()
            );

            assertTrue(FileType.INTERVIEW.isValidFile(file));
        }

        @ParameterizedTest
        @ValueSource(strings = {"interview.txt", "interview.doc", "interview.bpmn", "interview.mcd", "interview.xml"})
        @DisplayName("isValidFile rejects non-pdf extensions")
        void rejectsNonPdfExtension(String filename) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "application/octet-stream", "content".getBytes()
            );

            assertFalse(FileType.INTERVIEW.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile rejects file without extension")
        void rejectsFileWithoutExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "interview", "application/pdf", "content".getBytes()
            );

            assertFalse(FileType.INTERVIEW.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile rejects null filename")
        void rejectsNullFilename() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", null, "application/pdf", "content".getBytes()
            );

            assertFalse(FileType.INTERVIEW.isValidFile(file));
        }
    }

    @Nested
    @DisplayName("MCD File Type Tests")
    class McdFileTypeTests {

        @Test
        @DisplayName("isValidFile accepts .mcd extension")
        void acceptsMcdExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "model.mcd", "application/octet-stream", "mcd content".getBytes()
            );

            assertTrue(FileType.MCD.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile accepts .MCD uppercase extension")
        void acceptsMcdUppercaseExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "model.MCD", "application/octet-stream", "mcd content".getBytes()
            );

            assertTrue(FileType.MCD.isValidFile(file));
        }

        @ParameterizedTest
        @ValueSource(strings = {"model.txt", "model.xml", "model.pdf", "model.bpmn"})
        @DisplayName("isValidFile rejects non-mcd extensions")
        void rejectsNonMcdExtension(String filename) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "application/octet-stream", "content".getBytes()
            );

            assertFalse(FileType.MCD.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile rejects file without extension")
        void rejectsFileWithoutExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "model", "application/octet-stream", "content".getBytes()
            );

            assertFalse(FileType.MCD.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile rejects null filename")
        void rejectsNullFilename() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", null, "application/octet-stream", "content".getBytes()
            );

            assertFalse(FileType.MCD.isValidFile(file));
        }
    }

    @Nested
    @DisplayName("US (User Stories) File Type Tests")
    class UsFileTypeTests {

        @Test
        @DisplayName("isValidFile returns false for all extensions (unsupported type)")
        void usTypeNotSupported() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "stories.txt", "text/plain", "content".getBytes()
            );

            assertFalse(FileType.US.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile returns false for pdf files")
        void usPdfNotSupported() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "stories.pdf", "application/pdf", "content".getBytes()
            );

            assertFalse(FileType.US.isValidFile(file));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("isValidFile handles filename with multiple dots correctly")
        void handlesMultipleDots() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "my.diagram.bpmn", "application/xml", "<bpmn />".getBytes()
            );

            assertTrue(FileType.BPMN.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile handles filename with dots in name correctly")
        void handlesDotInName() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "my.special.interview.pdf", "application/pdf", "content".getBytes()
            );

            assertTrue(FileType.INTERVIEW.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile case insensitivity works with mixed case names")
        void caseInsensitivityWorks() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "MyDiagram.BPMN", "application/xml", "<bpmn />".getBytes()
            );

            assertTrue(FileType.BPMN.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile with empty extension (filename ends with dot)")
        void filenameEndsWithDot() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diagram.", "application/xml", "<bpmn />".getBytes()
            );

            assertFalse(FileType.BPMN.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile with long filename and correct extension")
        void longFilenameWithCorrectExtension() {
            String longName = "a".repeat(200) + ".bpmn";
            MockMultipartFile file = new MockMultipartFile(
                    "file", longName, "application/xml", "<bpmn />".getBytes()
            );

            assertTrue(FileType.BPMN.isValidFile(file));
        }

        @Test
        @DisplayName("isValidFile with special characters in filename")
        void specialCharactersInFilename() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "my-diagram_v2.0.bpmn", "application/xml", "<bpmn />".getBytes()
            );

            assertTrue(FileType.BPMN.isValidFile(file));
        }
    }

    @Nested
    @DisplayName("Type Validation Combinations Tests")
    class TypeValidationCombinationsTests {

        @Test
        @DisplayName("Wrong file type with BPMN extension fails MCD validation")
        void wrongTypeWithBpmnExtensionFailsMcd() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
            );

            assertTrue(FileType.BPMN.isValidFile(file));
            assertFalse(FileType.MCD.isValidFile(file));
        }

        @Test
        @DisplayName("MCD file with .mcd extension fails BPMN validation")
        void mcdFileFailsBpmnValidation() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "model.mcd", "application/octet-stream", "mcd".getBytes()
            );

            assertTrue(FileType.MCD.isValidFile(file));
            assertFalse(FileType.BPMN.isValidFile(file));
        }

        @Test
        @DisplayName("PDF file with .pdf extension fails BPMN validation")
        void pdfFileFailsBpmnValidation() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "interview.pdf", "application/pdf", "pdf".getBytes()
            );

            assertTrue(FileType.INTERVIEW.isValidFile(file));
            assertFalse(FileType.BPMN.isValidFile(file));
        }
    }
}





