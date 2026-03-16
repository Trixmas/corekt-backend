package lab.corekt.backend.modules.upload.service;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    @Test
    void uploadFileSavesMappedEntity() throws Exception {
        VersionEntity version = new VersionEntity();
        byte[] content = "hello".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "interview.pdf", "text/plain", "hello".getBytes());

        when(fileRepository.saveAndFlush(any(FileEntity.class))).thenAnswer(i -> i.getArgument(0));

        FileEntity result = fileService.uploadFile(version, file, FileType.INTERVIEW);

        assertEquals("interview.pdf", result.getName());
        assertEquals(FileType.INTERVIEW, result.getType());
        assertSame(version, result.getVersion());
        assertArrayEquals(content, result.getFile());
        verify(fileRepository).saveAndFlush(any(FileEntity.class));
    }

    @Test
    void uploadFileDuplicateTypeThrowsDataIntegrityViolationException() throws Exception {
        VersionEntity version = new VersionEntity();
        MockMultipartFile file = new MockMultipartFile("file", "interview.pdf", "text/plain", "hello".getBytes());
        DataIntegrityViolationException rootCause = new DataIntegrityViolationException("duplicate key");

        when(fileRepository.saveAndFlush(any(FileEntity.class))).thenThrow(rootCause);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> fileService.uploadFile(version, file, FileType.INTERVIEW)
        );

        assertTrue(thrown.getMessage().contains("already exists"));
        assertSame(rootCause, thrown.getCause());
        verify(fileRepository).saveAndFlush(any(FileEntity.class));
    }

    @Test
    void uploadFileNullTypeThrowsNullPointerException() {
        VersionEntity version = new VersionEntity();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        assertThrows(NullPointerException.class, () -> fileService.uploadFile(version, file, null));
        verifyNoInteractions(fileRepository);
    }

    @Test
    void uploadFileWhenReadFailsPropagatesIOException() throws Exception {
        VersionEntity version = new VersionEntity();
        MultipartFile file = mock(MultipartFile.class);

        when(file.getOriginalFilename()).thenReturn("broken.txt");
        when(file.getBytes()).thenThrow(new IOException("boom"));

        assertThrows(IOException.class, () -> fileService.uploadFile(version, file, FileType.INTERVIEW));
        verify(fileRepository, never()).saveAndFlush(any(FileEntity.class));
    }

    @Test
    void getOrCreateReturnsExistingFileWithoutSaving() {
        VersionEntity version = new VersionEntity();
        FileEntity existing = new FileEntity(FileType.INTERVIEW, "existing.pdf");
        existing.setVersion(version);

        when(fileRepository.findByTypeAndVersion(FileType.INTERVIEW, version)).thenReturn(Optional.of(existing));

        FileEntity result = fileService.getOrCreate(FileType.INTERVIEW, "new-name.pdf", version);

        assertSame(existing, result);
        verify(fileRepository).findByTypeAndVersion(FileType.INTERVIEW, version);
        verify(fileRepository, never()).save(any(FileEntity.class));
    }

    @Test
    void getOrCreateCreatesAndSavesWhenMissing() {
        VersionEntity version = new VersionEntity();

        when(fileRepository.findByTypeAndVersion(FileType.INTERVIEW, version)).thenReturn(Optional.empty());
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(i -> i.getArgument(0));

        FileEntity result = fileService.getOrCreate(FileType.INTERVIEW, "generated.pdf", version);

        assertEquals(FileType.INTERVIEW, result.getType());
        assertEquals("generated.pdf", result.getName());
        assertSame(version, result.getVersion());
        verify(fileRepository).findByTypeAndVersion(FileType.INTERVIEW, version);
        verify(fileRepository).save(any(FileEntity.class));
    }
}
