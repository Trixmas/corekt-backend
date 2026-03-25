package lab.corekt.backend.modules.upload.service;

import lab.corekt.backend.modules.mistral.service.MistralService;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.service.BpmnParserService;
import lab.corekt.backend.modules.project.parsing.service.McdParserService;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private BpmnParserService bpmnParserService;

    @Mock
    private McdParserService mcdParserService;

    @InjectMocks
    private FileService fileService;

    @Mock
    private MistralService mistralService;

    private VersionEntity version;

    @BeforeEach
    void setUp() {
        version = new VersionEntity();
        version.setId(java.util.UUID.randomUUID());
    }

    @Test
    @DisplayName("uploadFile saves file content through save")
    void uploadFileSavesFileContent() throws Exception {

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "interview.pdf", "text/plain", "hello".getBytes()
        );

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            entity.setId(java.util.UUID.randomUUID());
            return entity;
        });

        FileEntity result = fileService.uploadFile(version, multipartFile, FileType.INTERVIEW);

        ArgumentCaptor<FileEntity> captor = ArgumentCaptor.forClass(FileEntity.class);
        verify(fileRepository).save(captor.capture());

        FileEntity saved = captor.getValue();
        assertEquals("interview.pdf", saved.getName());
        assertEquals(FileType.INTERVIEW, saved.getType());
        assertSame(version, saved.getVersion());
        assertArrayEquals("hello".getBytes(), saved.getFile());

        verify(bpmnParserService, never()).processFile(any(FileEntity.class));
        verify(mcdParserService, never()).processFile(any(FileEntity.class));

        assertEquals(saved.getName(), result.getName());
        assertEquals(saved.getType(), result.getType());
    }

    @Test
    @DisplayName("uploadFile wraps multipart read failures")
    void uploadFileWrapsIoFailure() throws Exception {

        MultipartFile multipartFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("broken.mcd");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getBytes()).thenThrow(new IOException("disk read failed"));

        IOException exception = assertThrows(IOException.class,
                () -> fileService.uploadFile(version, multipartFile, FileType.MCD));

        assertEquals("Failed to read file content", exception.getMessage());
        assertEquals("disk read failed", exception.getCause().getMessage());
        verify(fileRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadFile wraps duplicate file constraint violations")
    void uploadFileWrapsConstraintViolation() throws Exception {

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
        );

        when(fileRepository.save(any(FileEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> fileService.uploadFile(version, multipartFile, FileType.BPMN));

        assertEquals(
                "Failed to save file, a file of type BPMN already exists for this version.",
                exception.getMessage()
        );
        assertEquals("duplicate key", exception.getCause().getMessage());
        verify(bpmnParserService).processFile(any(FileEntity.class));
        verify(mcdParserService, never()).processFile(any(FileEntity.class));
    }

    @Test
    @DisplayName("uploadFile dispatches BPMN files to BPMN parser")
    void uploadFileDispatchesBpmnParser() throws Exception {

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
        );

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileEntity result = fileService.uploadFile(version, multipartFile, FileType.BPMN);

        InOrder inOrder = inOrder(bpmnParserService, fileRepository);
        inOrder.verify(bpmnParserService).processFile(result);
        inOrder.verify(fileRepository).save(result);
        verify(mcdParserService, never()).processFile(any(FileEntity.class));
    }

    @Test
    @DisplayName("uploadFile dispatches MCD files to MCD parser")
    void uploadFileDispatchesMcdParser() throws Exception {

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "diagram.mcd", "application/octet-stream", "mcd".getBytes()
        );

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileEntity result = fileService.uploadFile(version, multipartFile, FileType.MCD);

        InOrder inOrder = inOrder(mcdParserService, fileRepository);
        inOrder.verify(mcdParserService).processFile(result);
        inOrder.verify(fileRepository).save(result);
        verify(bpmnParserService, never()).processFile(any(FileEntity.class));
    }

    @Test
    @DisplayName("uploadFile wraps BPMN parser failures and skips persistence")
    void uploadFileWrapsBpmnParserFailure() throws Exception {

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
        );

        doThrow(new IllegalStateException("invalid bpmn"))
                .when(bpmnParserService).processFile(any(FileEntity.class));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(version, multipartFile, FileType.BPMN));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        String reason = exception.getReason();
        assertTrue(reason != null && reason.contains("Failed to parse the uploaded file: invalid bpmn"));
        verify(fileRepository, never()).save(any(FileEntity.class));
        verify(mcdParserService, never()).processFile(any(FileEntity.class));
    }

    @Test
    @DisplayName("uploadFile wraps MCD parser failures and skips persistence")
    void uploadFileWrapsMcdParserFailure() throws Exception {

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "diagram.mcd", "application/octet-stream", "mcd".getBytes()
        );

        doThrow(new IllegalArgumentException("invalid mcd"))
                .when(mcdParserService).processFile(any(FileEntity.class));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(version, multipartFile, FileType.MCD));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        String reason = exception.getReason();
        assertTrue(reason != null && reason.contains("Failed to parse the uploaded file: invalid mcd"));
        verify(fileRepository, never()).save(any(FileEntity.class));
        verify(bpmnParserService, never()).processFile(any(FileEntity.class));
    }


    @Test
    @DisplayName("getOrCreate returns existing file when one already exists")
    void getOrCreateReturnsExistingFile() {

        FileEntity existing = new FileEntity(FileType.US, "User Stories");
        existing.setVersion(version);

        when(fileRepository.findByTypeAndVersion(FileType.US, version)).thenReturn(Optional.of(existing));

        FileEntity result = fileService.getOrCreate(FileType.US, "User Stories", version);

        assertSame(existing, result);
        verify(fileRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreate creates and saves a new file when missing")
    void getOrCreateCreatesNewFile() {


        when(fileRepository.findByTypeAndVersion(FileType.US, version)).thenReturn(Optional.empty());
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileEntity result = fileService.getOrCreate(FileType.US, "User Stories", version);

        ArgumentCaptor<FileEntity> captor = ArgumentCaptor.forClass(FileEntity.class);
        verify(fileRepository).save(captor.capture());

        FileEntity saved = captor.getValue();
        assertEquals(FileType.US, saved.getType());
        assertEquals("User Stories", saved.getName());
        assertSame(version, saved.getVersion());
        assertSame(saved, result);
    }

    @Test
    @DisplayName("uploadFile rejects empty files")
    void uploadFileRejectsEmptyFile() {

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(version, emptyFile, FileType.INTERVIEW));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("File is empty", exception.getReason());
        verify(fileRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadFile rejects files exceeding 10MB limit")
    void uploadFileRejectsOversizedFile() {

        MultipartFile oversizedFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[Math.toIntExact(11L * 1024 * 1024)]);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(version, oversizedFile, FileType.INTERVIEW));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("File size exceeds 10MB limit", exception.getReason());
        verify(fileRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadFile rejects invalid file type extension")
    void uploadFileRejectsInvalidFileExtension() {

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "wrong.txt", "text/plain", "content".getBytes()
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(version, invalidFile, FileType.INTERVIEW));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String reason = exception.getReason();
        assertTrue(reason != null && reason.contains("Invalid file type or extension"));
        verify(fileRepository, never()).save(any());
    }


    @Test
    @DisplayName("uploadFile successfully processes valid PDF interview file")
    void uploadFileSavesValidPdfFile() throws Exception {

        byte[] pdfContent = "PDF mock content".getBytes();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "interview.pdf", "application/pdf", pdfContent
        );

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileEntity result = fileService.uploadFile(version, pdfFile, FileType.INTERVIEW);

        assertEquals("interview.pdf", result.getName());
        assertEquals(FileType.INTERVIEW, result.getType());
        assertArrayEquals(pdfContent, result.getFile());
        verify(fileRepository).save(any(FileEntity.class));
    }
}
