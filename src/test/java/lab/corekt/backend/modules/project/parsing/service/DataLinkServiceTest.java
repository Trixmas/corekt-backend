package lab.corekt.backend.modules.project.parsing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lab.corekt.backend.modules.mistral.service.MistralService;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.repository.DataLinkRepository;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;

@ExtendWith(MockitoExtension.class)
class DataLinkServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ParsedDataRepository parsedDataRepository;

    @Mock
    private DataLinkRepository dataLinkRepository;

    @Mock
    private MistralService mistralService;

    @InjectMocks
    private DataLinkService dataLinkService;

    @Test
    void testGetRequiredFiles() {
        VersionEntity version = new VersionEntity();
        Map<FileType, FileEntity> filesByType = stubAllFiles(version);

        Map<FileType, FileEntity> files = dataLinkService.getRequiredFiles(version);

        assertEquals(FileType.values().length, files.size());
        assertEquals(filesByType.get(FileType.INTERVIEW), files.get(FileType.INTERVIEW));
        assertEquals(filesByType.get(FileType.BPMN), files.get(FileType.BPMN));
        assertEquals(filesByType.get(FileType.US), files.get(FileType.US));
        assertEquals(filesByType.get(FileType.MCD), files.get(FileType.MCD));
    }

    @Test
    void testGetRequiredFilesMissingDocumentThrows() {
        VersionEntity version = new VersionEntity();
        List<FileEntity> files = List.of(
                new FileEntity(FileType.INTERVIEW, "interview.pdf"),
                new FileEntity(FileType.BPMN, "process.bpmn"),
                new FileEntity(FileType.US, "user-stories"));

        FileEntity interviewFile = new FileEntity(FileType.INTERVIEW, "interview.pdf");
        FileEntity bpmnFile = new FileEntity(FileType.BPMN, "process.bpmn");
        FileEntity usFile = new FileEntity(FileType.US, "user-stories");
        when(fileRepository.findByVersion(version)).thenReturn(List.of(interviewFile, bpmnFile, usFile));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> dataLinkService.getRequiredFiles(version));

        assertEquals("Missing required files for version: MCD", exception.getMessage());
    }

    @Test
    void testGetParsedDataByFileType() {
        VersionEntity version = new VersionEntity();
        Map<FileType, FileEntity> filesByType = stubAllFiles(version);
        List<ParsedDataEntity> parsedDataForVersion = new java.util.ArrayList<>();

        for (Map.Entry<FileType, FileEntity> entry : filesByType.entrySet()) {
            ParsedDataEntity parsedData = new ParsedDataEntity(DataType.ACTION, entry.getKey().name().toLowerCase());
            parsedData.setFile(entry.getValue());
            parsedDataForVersion.add(parsedData);
        }

        when(parsedDataRepository.findByVersion(version)).thenReturn(parsedDataForVersion);

        Map<FileType, List<ParsedDataEntity>> parsedDataByFileType = dataLinkService.getParsedDataByFileType(version);

        assertEquals(FileType.values().length, parsedDataByFileType.size());
        assertEquals("interview", parsedDataByFileType.get(FileType.INTERVIEW).getFirst().getContent());
        assertEquals("bpmn", parsedDataByFileType.get(FileType.BPMN).getFirst().getContent());
        assertEquals("us", parsedDataByFileType.get(FileType.US).getFirst().getContent());
        assertEquals("mcd", parsedDataByFileType.get(FileType.MCD).getFirst().getContent());
    }

    @Test
    void testGetEmbeddingsByFileType() {
        VersionEntity version = new VersionEntity();
        Map<FileType, FileEntity> filesByType = stubAllFiles(version);
        List<ParsedDataEntity> parsedDataForVersion = new java.util.ArrayList<>();

        for (Map.Entry<FileType, FileEntity> entry : filesByType.entrySet()) {
            ParsedDataEntity parsedData = new ParsedDataEntity(DataType.ACTION, entry.getKey().name().toLowerCase());
            parsedData.setFile(entry.getValue());
            parsedDataForVersion.add(parsedData);

            MistralService.EmbeddingTuple embeddingTuple = new MistralService.EmbeddingTuple(parsedData,
                    new float[] { 1.0f });
            when(mistralService.embedParsedData(List.of(parsedData))).thenReturn(List.of(embeddingTuple));
        }

        when(parsedDataRepository.findByVersion(version)).thenReturn(parsedDataForVersion);

        Map<FileType, List<MistralService.EmbeddingTuple>> embeddingsByFileType = dataLinkService
                .getEmbeddingsByFileType(version);

        assertEquals(FileType.values().length, embeddingsByFileType.size());
        assertEquals("interview", embeddingsByFileType.get(FileType.INTERVIEW).getFirst().parsedData().getContent());
        assertEquals("bpmn", embeddingsByFileType.get(FileType.BPMN).getFirst().parsedData().getContent());
        assertEquals("us", embeddingsByFileType.get(FileType.US).getFirst().parsedData().getContent());
        assertEquals("mcd", embeddingsByFileType.get(FileType.MCD).getFirst().parsedData().getContent());

        verify(mistralService).embedParsedData(embeddingsByFileType.get(FileType.INTERVIEW).stream()
                .map(MistralService.EmbeddingTuple::parsedData).toList());
        verify(mistralService).embedParsedData(embeddingsByFileType.get(FileType.BPMN).stream()
                .map(MistralService.EmbeddingTuple::parsedData).toList());
        verify(mistralService).embedParsedData(embeddingsByFileType.get(FileType.US).stream()
                .map(MistralService.EmbeddingTuple::parsedData).toList());
        verify(mistralService).embedParsedData(embeddingsByFileType.get(FileType.MCD).stream()
                .map(MistralService.EmbeddingTuple::parsedData).toList());
    }

    @Test
    void testBuildDataLinksUsesCompatibilityMatrixOnly() {
        VersionEntity version = new VersionEntity();
        Map<FileType, FileEntity> filesByType = stubAllFiles(version);

        ParsedDataEntity interviewActor = parsedData(filesByType.get(FileType.INTERVIEW), DataType.ACTOR, "manager");
        ParsedDataEntity interviewAction = parsedData(filesByType.get(FileType.INTERVIEW), DataType.ACTION,
                "valider une demande");
        ParsedDataEntity interviewEntity = parsedData(filesByType.get(FileType.INTERVIEW), DataType.ENTITY,
                "absence");

        ParsedDataEntity bpmnActor = parsedData(filesByType.get(FileType.BPMN), DataType.ACTOR, "manager");
        ParsedDataEntity bpmnAction = parsedData(filesByType.get(FileType.BPMN), DataType.ACTION,
                "valider une demande");
        ParsedDataEntity bpmnFlow = parsedData(filesByType.get(FileType.BPMN), DataType.FLOW, "debut -> fin");

        ParsedDataEntity userStory = parsedData(filesByType.get(FileType.US), DataType.US, "valider une demande");
        ParsedDataEntity mcdEntity = parsedData(filesByType.get(FileType.MCD), DataType.ENTITY, "absence");

        when(parsedDataRepository.findByVersion(version)).thenReturn(List.of(
                interviewActor, interviewAction, interviewEntity,
                bpmnActor, bpmnAction, bpmnFlow,
                userStory,
                mcdEntity));

        when(mistralService.embedParsedData(List.of(interviewActor, interviewAction, interviewEntity)))
                .thenReturn(List.of(
                        tuple(interviewActor, 1.0f, 0.0f),
                        tuple(interviewAction, 0.0f, 1.0f),
                        tuple(interviewEntity, 0.5f, 0.5f)));
        when(mistralService.embedParsedData(List.of(bpmnActor, bpmnAction, bpmnFlow)))
                .thenReturn(List.of(
                        tuple(bpmnActor, 1.0f, 0.0f),
                        tuple(bpmnAction, 0.0f, 1.0f),
                        tuple(bpmnFlow, 0.3f, 0.7f)));
        when(mistralService.embedParsedData(List.of(userStory)))
                .thenReturn(List.of(tuple(userStory, 0.0f, 1.0f)));
        when(mistralService.embedParsedData(List.of(mcdEntity)))
                .thenReturn(List.of(tuple(mcdEntity, 0.5f, 0.5f)));

        List<DataLinkEntity> dataLinks = dataLinkService.buildDataLinks(version);

        assertEquals(8, dataLinks.size());
        assertTrue(hasExactLink(dataLinks, bpmnActor, interviewActor));
        assertTrue(hasExactLink(dataLinks, interviewActor, bpmnActor));
        assertTrue(hasExactLink(dataLinks, bpmnAction, interviewAction));
        assertTrue(hasExactLink(dataLinks, userStory, interviewAction));
        assertTrue(hasExactLink(dataLinks, mcdEntity, interviewEntity));

        assertTrue(dataLinks.stream().anyMatch(link -> link.getSourceParsedData() == bpmnFlow
                && link.getMatch() == MatchType.MISSING));

        // ACTOR(BPMN) cannot target ACTION(Interview) even with a perfect embedding match.
        assertTrue(dataLinks.stream().noneMatch(link -> link.getSourceParsedData() == bpmnActor
                && link.getTargetParsedData() == interviewAction
                && link.getMatch() != MatchType.MISSING));
    }

    @Test
    void testSaveDataLinks() {
        VersionEntity version = new VersionEntity();
        Map<FileType, FileEntity> filesByType = stubAllFiles(version);

        ParsedDataEntity interviewActor = parsedData(filesByType.get(FileType.INTERVIEW), DataType.ACTOR, "manager");
        ParsedDataEntity bpmnActor = parsedData(filesByType.get(FileType.BPMN), DataType.ACTOR, "manager");
        ParsedDataEntity userStory = parsedData(filesByType.get(FileType.US), DataType.US, "En tant que manager...");
        ParsedDataEntity entity = parsedData(filesByType.get(FileType.MCD), DataType.ENTITY, "absence");

        when(parsedDataRepository.findByVersion(version)).thenReturn(List.of(interviewActor, bpmnActor, userStory, entity));

        when(mistralService.embedParsedData(List.of(interviewActor)))
                .thenReturn(List.of(tuple(interviewActor, 1.0f, 0.0f)));
        when(mistralService.embedParsedData(List.of(bpmnActor)))
                .thenReturn(List.of(tuple(bpmnActor, 1.0f, 0.0f)));
        when(mistralService.embedParsedData(List.of(userStory)))
                .thenReturn(List.of(tuple(userStory, 0.0f, 1.0f)));
        when(mistralService.embedParsedData(List.of(entity)))
                .thenReturn(List.of(tuple(entity, 0.5f, 0.5f)));

        when(dataLinkRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<DataLinkEntity> savedLinks = dataLinkService.saveDataLinks(version);

        assertEquals(4, savedLinks.size());
        verify(dataLinkRepository).saveAll(anyList());
    }

    private Map<FileType, FileEntity> stubAllFiles(VersionEntity version) {
        Map<FileType, FileEntity> filesByType = new EnumMap<>(FileType.class);
        for (FileType fileType : FileType.values()) {
            filesByType.put(fileType, new FileEntity(fileType, fileType.name().toLowerCase()));
        }

        when(fileRepository.findByVersion(version)).thenReturn(List.copyOf(filesByType.values()));
        return filesByType;
    }

    private ParsedDataEntity parsedData(FileEntity file, DataType dataType, String content) {
        ParsedDataEntity parsedData = new ParsedDataEntity(dataType, content);
        parsedData.setFile(file);
        return parsedData;
    }

    private MistralService.EmbeddingTuple tuple(ParsedDataEntity parsedData, float left, float right) {
        return new MistralService.EmbeddingTuple(parsedData, new float[] { left, right });
    }

    private boolean hasExactLink(List<DataLinkEntity> links, ParsedDataEntity source, ParsedDataEntity target) {
        return links.stream().anyMatch(link -> link.getSourceParsedData() == source
                && link.getTargetParsedData() == target
                && link.getMatch() == MatchType.EXACT);
    }
}
