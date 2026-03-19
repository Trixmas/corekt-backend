package lab.corekt.backend.modules.project.parsing.service;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class McdParserServiceTest {

    @Mock
    private ParsedDataRepository parsedDataRepository;

    @InjectMocks
    private McdParserService mcdParserService;

    @Test
    @DisplayName("Should parse and save entities and relations from a valid file")
    void testShouldProcessValidFile() throws Exception {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(UUID.randomUUID());

        List<Object> mockItems = List.of(
                new MockIhmEntite("Entity1"),
                new MockIhmRelation("Relation1"),
                new MockIhmEntite("Entity2")
        );
        fileEntity.setFile(serializeTestData(mockItems));

        mcdParserService.processFile(fileEntity);

        ArgumentCaptor<List<ParsedDataEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(parsedDataRepository).saveAll(captor.capture());

        List<ParsedDataEntity> saved = captor.getValue();
        assertEquals(3, saved.size());
        assertEquals(2, saved.stream().filter(e -> e.getDataType() == DataType.ENTITY).count());
        assertEquals(1, saved.stream().filter(e -> e.getDataType() == DataType.RELATION).count());
        assertTrue(saved.stream().allMatch(e -> e.getFile().equals(fileEntity)));
    }

    @Test
    @DisplayName("Should truncate content if it exceeds MAX_CONTENT_LENGTH")
    void testShouldTruncateLongContent() throws Exception {
        String longName = "A".repeat(600);
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFile(serializeTestData(List.of(new MockIhmEntite(longName))));

        mcdParserService.processFile(fileEntity);

        ArgumentCaptor<List<ParsedDataEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(parsedDataRepository).saveAll(captor.capture());

        String savedContent = captor.getValue().get(0).getContent();
        assertEquals(500, savedContent.length());
        assertTrue(longName.startsWith(savedContent));
    }

    @Test
    @DisplayName("Should handle null file data")
    void testShouldHandleNullFile() throws Exception {
        FileEntity fileEntity = new FileEntity();

        fileEntity.setFile(null);
        mcdParserService.processFile(fileEntity);


        verify(parsedDataRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle empty file data")
    void testShouldHandleEmptyFile() throws Exception {
        FileEntity fileEntity = new FileEntity();

        fileEntity.setFile(new byte[0]);
        mcdParserService.processFile(fileEntity);

        verify(parsedDataRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw exception for corrupted serialized data")
    void testShouldThrowOnInvalidData() {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFile("corrupted".getBytes());

        assertThrows(Exception.class, () -> mcdParserService.processFile(fileEntity));
    }

    // --- UTILS ---

    private byte[] serializeTestData(List<Object> items) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject("dummy_index_0");
        oos.writeObject(items);
        oos.close();
        return baos.toByteArray();
    }

    // --- Mock ---
    private record MockIhmEntite(String name) implements Serializable {
        public McdParserServiceTest.MockEntite getEntite() {
            return new McdParserServiceTest.MockEntite(name);
        }
    }

    private record MockIhmRelation(String name) implements Serializable {
        public McdParserServiceTest.MockRelation getRelation() {
            return new McdParserServiceTest.MockRelation(name);
        }
    }

    private record MockEntite(String name) implements Serializable {
        public String getNom() {
            return name;
        }
    }

    private record MockRelation(String name) implements Serializable {
        public String getNom() {
            return name;
        }
    }
}
