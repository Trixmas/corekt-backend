package lab.corekt.backend.modules.upload.service;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.service.BpmnParserService;
import lab.corekt.backend.modules.project.parsing.service.McdParserService;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("uploadFile saves file content through save")
    void uploadFileSavesFileContent() throws Exception {
        VersionEntity version = new VersionEntity("Version 1");
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
        VersionEntity version = new VersionEntity("Version 1");
        MultipartFile multipartFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("broken.txt");
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
        VersionEntity version = new VersionEntity("Version 1");
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
        verify(bpmnParserService, never()).processFile(any(FileEntity.class));
        verify(mcdParserService, never()).processFile(any(FileEntity.class));
    }

    @Test
    @DisplayName("uploadFile dispatches BPMN files to BPMN parser")
    void uploadFileDispatchesBpmnParser() throws Exception {
        VersionEntity version = new VersionEntity("Version 1");
        MockMultipartFile multipartFile = new MockMultipartFile(
            "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
        );

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileEntity result = fileService.uploadFile(version, multipartFile, FileType.BPMN);

        verify(fileRepository).save(any(FileEntity.class));
        verify(bpmnParserService).processFile(result);
        verify(mcdParserService, never()).processFile(any(FileEntity.class));
    }

    @Test
    @DisplayName("uploadFile dispatches MCD files to MCD parser")
    void uploadFileDispatchesMcdParser() throws Exception {
        VersionEntity version = new VersionEntity("Version 1");
        MockMultipartFile multipartFile = new MockMultipartFile(
            "file", "diagram.mcd", "application/octet-stream", "mcd".getBytes()
        );

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileEntity result = fileService.uploadFile(version, multipartFile, FileType.MCD);

        verify(fileRepository).save(any(FileEntity.class));
        verify(mcdParserService).processFile(result);
        verify(bpmnParserService, never()).processFile(any(FileEntity.class));
    }

    @Test
    @DisplayName("uploadFile deletes persisted file and throws when parsing fails")
    void uploadFileDeletesFileWhenParsingFails() throws Exception {
        VersionEntity version = new VersionEntity("Version 1");
        MockMultipartFile multipartFile = new MockMultipartFile(
            "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
        );

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            entity.setId(java.util.UUID.randomUUID());
            return entity;
        });
        doThrow(new RuntimeException("parse boom")).when(bpmnParserService).processFile(any(FileEntity.class));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> fileService.uploadFile(version, multipartFile, FileType.BPMN));

        assertEquals(500, exception.getStatusCode().value());
        assertEquals("Failed to parse the uploaded file: parse boom", exception.getReason());

        ArgumentCaptor<FileEntity> savedCaptor = ArgumentCaptor.forClass(FileEntity.class);
        ArgumentCaptor<FileEntity> deletedCaptor = ArgumentCaptor.forClass(FileEntity.class);
        verify(fileRepository).save(savedCaptor.capture());
        verify(fileRepository).delete(deletedCaptor.capture());
        assertSame(savedCaptor.getValue(), deletedCaptor.getValue());
    }

    @Test
    @DisplayName("getOrCreate returns existing file when one already exists")
    void getOrCreateReturnsExistingFile() {
        VersionEntity version = new VersionEntity("Version 1");
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
        VersionEntity version = new VersionEntity("Version 1");

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
}
