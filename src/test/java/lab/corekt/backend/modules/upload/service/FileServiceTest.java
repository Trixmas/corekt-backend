package lab.corekt.backend.modules.upload.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    @Test
    void testUploadFile() throws Exception {
        VersionEntity version = new VersionEntity();
        MockMultipartFile file = new MockMultipartFile("file", "interview.pdf", "text/plain", "hello".getBytes());

        when(fileRepository.existsByVersionAndType(version, FileType.INTERVIEW)).thenReturn(false);
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(i -> i.getArgument(0));

        FileEntity result = fileService.uploadFile(version, file, FileType.INTERVIEW);

        assertEquals("interview.pdf", result.getName());
        assertEquals(FileType.INTERVIEW, result.getType());
    }

    @Test
    void testUploadFileDuplicateThrows() {
        VersionEntity version = new VersionEntity();
        MockMultipartFile file = new MockMultipartFile("file", "interview.pdf", "text/plain", "hello".getBytes());

        when(fileRepository.existsByVersionAndType(version, FileType.INTERVIEW)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            fileService.uploadFile(version, file, FileType.INTERVIEW);
        });
    }

    @Test
    void testUploadFileTypeIsInstanceOfFileType() throws Exception {
        VersionEntity version = new VersionEntity();
        when(fileRepository.existsByVersionAndType(any(), any())).thenReturn(false);
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "interview.pdf", "text/plain", "hello".getBytes());
        FileEntity resultInterview = fileService.uploadFile(version, file, FileType.INTERVIEW);
        assertInstanceOf(FileType.class, resultInterview.getType());
        assertEquals(FileType.INTERVIEW, resultInterview.getType());
    }

    @Test
    void testUploadFileNullTypeThrows() {
        VersionEntity version = new VersionEntity();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        assertThrows(NullPointerException.class, () -> {
            fileService.uploadFile(version, file, null);
        });
    }
}
