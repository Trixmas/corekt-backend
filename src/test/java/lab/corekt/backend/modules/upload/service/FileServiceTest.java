package lab.corekt.backend.modules.upload.service;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

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
    void testUploadFile() throws Exception {
        VersionEntity version = mock(VersionEntity.class);
        MockMultipartFile file = new MockMultipartFile("file", "interview.pdf", "text/plain", "hello".getBytes());

        when(fileRepository.existsByVersionAndType(version, FileType.INTERVIEW)).thenReturn(false);
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(i -> i.getArgument(0));

        FileEntity result = fileService.uploadFile(version, file, FileType.INTERVIEW);

        assertEquals("interview.pdf", result.getName());
        assertEquals(FileType.INTERVIEW, result.getType());
        assertEquals(version, result.getVersion());
        assertArrayEquals("hello".getBytes(), result.getFile());

        verify(fileRepository).save(any(FileEntity.class));
    }

    @Test
    void testUploadFileDuplicateThrows() {
        VersionEntity version = mock(VersionEntity.class);
        MockMultipartFile file = new MockMultipartFile("file", "interview.pdf", "text/plain", "hello".getBytes());

        when(fileRepository.existsByVersionAndType(version, FileType.INTERVIEW)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            fileService.uploadFile(version, file, FileType.INTERVIEW);
        });

        assertEquals("A file of type INTERVIEW already exists for this version", ex.getMessage());
        verify(fileRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(FileType.class)
    void testUploadFileTypeIsInstanceOfFileType(FileType fileType) throws Exception {
        VersionEntity version = mock(VersionEntity.class);

        when(fileRepository.existsByVersionAndType(any(), any())).thenReturn(false);
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "text/plain", "hello".getBytes());

        FileEntity result = fileService.uploadFile(version, file, fileType);

        assertEquals(fileType, result.getType());
    }

    @Test
    void testUploadFileNullTypeThrows() {
        VersionEntity version = mock(VersionEntity.class);
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        assertThrows(NullPointerException.class, () -> {
            fileService.uploadFile(version, file, null);
        });
    }

    @Test
    void testUploadBpmnFile() throws Exception {
        VersionEntity version = mock(VersionEntity.class);
        MockMultipartFile file = new MockMultipartFile("file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes());

        when(fileRepository.existsByVersionAndType(version, FileType.BPMN)).thenReturn(false);
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(i -> i.getArgument(0));

        FileEntity result = fileService.uploadFile(version, file, FileType.BPMN);

        assertEquals(FileType.BPMN, result.getType());
        assertEquals("diagram.bpmn", result.getName());
    }

    @Test
    void testUploadMcdFile() throws Exception {
        VersionEntity version = mock(VersionEntity.class);
        MockMultipartFile file = new MockMultipartFile("file", "schema.mcd", "text/plain", "MCD".getBytes());

        when(fileRepository.existsByVersionAndType(version, FileType.MCD)).thenReturn(false);
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(i -> i.getArgument(0));

        FileEntity result = fileService.uploadFile(version, file, FileType.MCD);

        assertEquals(FileType.MCD, result.getType());
        assertEquals("schema.mcd", result.getName());
    }

    @Test
    void testUploadBpmnDuplicateThrows() {
        VersionEntity version = mock(VersionEntity.class);
        MockMultipartFile file = new MockMultipartFile("file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes());

        when(fileRepository.existsByVersionAndType(version, FileType.BPMN)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> fileService.uploadFile(version, file, FileType.BPMN));

        assertEquals("A file of type BPMN already exists for this version", ex.getMessage());
        verify(fileRepository, never()).save(any());
    }

    @Test
    void testUploadMcdDuplicateThrows() {
        VersionEntity version = mock(VersionEntity.class);
        MockMultipartFile file = new MockMultipartFile("file", "schema.mcd", "text/plain", "MCD".getBytes());

        when(fileRepository.existsByVersionAndType(version, FileType.MCD)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> fileService.uploadFile(version, file, FileType.MCD));

        assertEquals("A file of type MCD already exists for this version", ex.getMessage());
        verify(fileRepository, never()).save(any());
    }
}
