package lab.corekt.backend.modules.project.parsing.service;

import lab.corekt.backend.modules.upload.entity.FileEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class McdParserReaderTest {

    @Mock
    private FileEntity mockFileEntity;

    private McdParserReader mcdParserReader;

    @BeforeEach
    void setUp() {
        mcdParserReader = new McdParserReader(mockFileEntity);
    }

    @Test
    @DisplayName("Should handle null file data gracefully")
    void shouldHandleNullFileData() throws Exception {
        when(mockFileEntity.getFile()).thenReturn(null);
        assertDoesNotThrow(() -> mcdParserReader.parseFile());
    }

    @Test
    @DisplayName("Should handle empty file data gracefully")
    void shouldHandleEmptyFileData() throws Exception {
        when(mockFileEntity.getFile()).thenReturn(new byte[0]);
        assertDoesNotThrow(() -> mcdParserReader.parseFile());

        assertTrue(mcdParserReader.getEntities().isEmpty());
    }

    @Test
    @DisplayName("Should parse entities and relations from a valid serialized stream")
    void shouldParseValidMcdData() throws Exception {
        List<Object> mockItems = List.of(
                new MockIhmEntite("Entity1"),
                new MockIhmRelation("Relation1"),
                new MockIhmEntite("Entity2")
        );
        byte[] serializedData = serializeTestData(mockItems);
        when(mockFileEntity.getFile()).thenReturn(serializedData);

        mcdParserReader.parseFile();

        assertEquals(2, mcdParserReader.getEntities().size());
        assertEquals(1, mcdParserReader.getRelations().size());
        assertTrue(mcdParserReader.getEntities().contains("Entity1"));
        assertTrue(mcdParserReader.getEntities().contains("Entity2"));
        assertTrue(mcdParserReader.getRelations().contains("Relation1"));
    }

    @Test
    @DisplayName("Should throw exception if data is not a valid Java serialization")
    void shouldThrowOnCorruptedData() {
        when(mockFileEntity.getFile()).thenReturn("Not A Serialized Object".getBytes());
        assertThrows(Exception.class, () -> mcdParserReader.parseFile());
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
        public MockEntite getEntite() {
            return new MockEntite(name);
        }
    }

    private record MockIhmRelation(String name) implements Serializable {
        public MockRelation getRelation() {
            return new MockRelation(name);
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
