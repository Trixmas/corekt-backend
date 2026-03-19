package lab.corekt.backend.modules.project.parsing.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
        List<FileEntity> files = List.of(
                new FileEntity(FileType.INTERVIEW, "interview"),
                new FileEntity(FileType.BPMN, "bpmn"),
                new FileEntity(FileType.US, "us"),
                new FileEntity(FileType.MCD, "mcd"));

        when(fileRepository.findByVersion(version)).thenReturn(files);

        Map<FileType, FileEntity> filesByType = dataLinkService.getRequiredFiles(version);

        assertEquals(FileType.values().length, filesByType.size());
        assertEquals(FileType.INTERVIEW, filesByType.get(FileType.INTERVIEW).getType());
        assertEquals(FileType.BPMN, filesByType.get(FileType.BPMN).getType());
        assertEquals(FileType.US, filesByType.get(FileType.US).getType());
        assertEquals(FileType.MCD, filesByType.get(FileType.MCD).getType());
    }

    @Test
    void testGetRequiredFilesMissingDocumentThrows() {
        VersionEntity version = new VersionEntity();
        List<FileEntity> files = List.of(
                new FileEntity(FileType.INTERVIEW, "interview.pdf"),
                new FileEntity(FileType.BPMN, "process.bpmn"),
                new FileEntity(FileType.US, "user-stories"));

        when(fileRepository.findByVersion(version)).thenReturn(files);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> dataLinkService.getRequiredFiles(version));

        assertEquals("Missing required files for version: MCD", exception.getMessage());
    }

    @Test
    void testGetParsedDataByFileType() {
        VersionEntity version = new VersionEntity();
        FileEntity interviewFile = new FileEntity(FileType.INTERVIEW, "interview");
        FileEntity bpmnFile = new FileEntity(FileType.BPMN, "bpmn");
        FileEntity usFile = new FileEntity(FileType.US, "us");
        FileEntity mcdFile = new FileEntity(FileType.MCD, "mcd");

        when(fileRepository.findByVersion(version)).thenReturn(List.of(interviewFile, bpmnFile, usFile, mcdFile));

        ParsedDataEntity interviewData = new ParsedDataEntity(DataType.ACTION, "interview");
        interviewData.setFile(interviewFile);
        ParsedDataEntity bpmnData = new ParsedDataEntity(DataType.ACTION, "bpmn");
        bpmnData.setFile(bpmnFile);
        ParsedDataEntity usData = new ParsedDataEntity(DataType.ACTION, "us");
        usData.setFile(usFile);
        ParsedDataEntity mcdData = new ParsedDataEntity(DataType.ACTION, "mcd");
        mcdData.setFile(mcdFile);

        when(parsedDataRepository.findByVersion(version))
                .thenReturn(List.of(interviewData, bpmnData, usData, mcdData));

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
        FileEntity interviewFile = new FileEntity(FileType.INTERVIEW, "interview");
        FileEntity bpmnFile = new FileEntity(FileType.BPMN, "bpmn");
        FileEntity usFile = new FileEntity(FileType.US, "us");
        FileEntity mcdFile = new FileEntity(FileType.MCD, "mcd");

        when(fileRepository.findByVersion(version)).thenReturn(List.of(interviewFile, bpmnFile, usFile, mcdFile));

        ParsedDataEntity interviewData = new ParsedDataEntity(DataType.ACTION, "interview");
        interviewData.setFile(interviewFile);
        ParsedDataEntity bpmnData = new ParsedDataEntity(DataType.ACTION, "bpmn");
        bpmnData.setFile(bpmnFile);
        ParsedDataEntity usData = new ParsedDataEntity(DataType.ACTION, "us");
        usData.setFile(usFile);
        ParsedDataEntity mcdData = new ParsedDataEntity(DataType.ACTION, "mcd");
        mcdData.setFile(mcdFile);

        when(parsedDataRepository.findByVersion(version))
                .thenReturn(List.of(interviewData, bpmnData, usData, mcdData));

        when(mistralService.embedParsedData(List.of(interviewData)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(interviewData, new float[] { 1.0f })));
        when(mistralService.embedParsedData(List.of(bpmnData)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(bpmnData, new float[] { 1.0f })));
        when(mistralService.embedParsedData(List.of(usData)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(usData, new float[] { 1.0f })));
        when(mistralService.embedParsedData(List.of(mcdData)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(mcdData, new float[] { 1.0f })));

        Map<FileType, List<MistralService.EmbeddingTuple>> embeddingsByFileType = dataLinkService
                .getEmbeddingsByFileType(version);

        assertEquals(FileType.values().length, embeddingsByFileType.size());
        assertEquals("interview", embeddingsByFileType.get(FileType.INTERVIEW).getFirst().parsedData().getContent());
        assertEquals("bpmn", embeddingsByFileType.get(FileType.BPMN).getFirst().parsedData().getContent());
        assertEquals("us", embeddingsByFileType.get(FileType.US).getFirst().parsedData().getContent());
        assertEquals("mcd", embeddingsByFileType.get(FileType.MCD).getFirst().parsedData().getContent());

        for (FileType fileType : FileType.values()) {
            verify(mistralService).embedParsedData(embeddingsByFileType.get(fileType).stream()
                    .map(MistralService.EmbeddingTuple::parsedData)
                    .toList());
        }
    }

    @Test
    void testBuildDataLinks() {
        VersionEntity version = new VersionEntity();

        FileEntity interviewFile = new FileEntity(FileType.INTERVIEW, "interview");
        FileEntity bpmnFile = new FileEntity(FileType.BPMN, "bpmn");
        FileEntity usFile = new FileEntity(FileType.US, "us");
        FileEntity mcdFile = new FileEntity(FileType.MCD, "mcd");

        ParsedDataEntity interviewActor = new ParsedDataEntity(DataType.ACTOR, "manager");
        interviewActor.setFile(interviewFile);

        ParsedDataEntity bpmnActor = new ParsedDataEntity(DataType.ACTOR, "manager");
        bpmnActor.setFile(bpmnFile);

        ParsedDataEntity userStory = new ParsedDataEntity(DataType.US, "En tant que manager...");
        userStory.setFile(usFile);

        ParsedDataEntity entity = new ParsedDataEntity(DataType.ENTITY, "absence");
        entity.setFile(mcdFile);

        when(fileRepository.findByVersion(version)).thenReturn(List.of(interviewFile, bpmnFile, usFile, mcdFile));
        when(parsedDataRepository.findByVersion(version))
                .thenReturn(List.of(interviewActor, bpmnActor, userStory, entity));

        when(mistralService.embedParsedData(List.of(interviewActor)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(interviewActor, new float[] { 1.0f, 0.0f })));
        when(mistralService.embedParsedData(List.of(bpmnActor)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(bpmnActor, new float[] { 1.0f, 0.0f })));
        when(mistralService.embedParsedData(List.of(userStory)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(userStory, new float[] { 0.0f, 1.0f })));
        when(mistralService.embedParsedData(List.of(entity)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(entity, new float[] { 0.5f, 0.5f })));

        List<DataLinkEntity> dataLinks = dataLinkService.buildDataLinks(version);

        assertEquals(4, dataLinks.size());
        long exactLinks = dataLinks.stream()
                .filter(link -> link.getMatch() == MatchType.EXACT)
                .filter(link -> "manager".equals(link.getSourceParsedData().getContent()))
                .filter(link -> "manager".equals(link.getTargetParsedData().getContent()))
                .count();

        assertEquals(2, exactLinks);

        long missingLinks = dataLinks.stream()
                .filter(link -> link.getMatch() == MatchType.MISSING)
                .count();

        assertEquals(2, missingLinks);
    }

    @Test
    void testSaveDataLinks() {
        VersionEntity version = new VersionEntity();

        FileEntity interviewFile = new FileEntity(FileType.INTERVIEW, "interview");
        FileEntity bpmnFile = new FileEntity(FileType.BPMN, "bpmn");
        FileEntity usFile = new FileEntity(FileType.US, "us");
        FileEntity mcdFile = new FileEntity(FileType.MCD, "mcd");

        ParsedDataEntity interviewActor = new ParsedDataEntity(DataType.ACTOR, "manager");
        interviewActor.setFile(interviewFile);

        ParsedDataEntity bpmnActor = new ParsedDataEntity(DataType.ACTOR, "manager");
        bpmnActor.setFile(bpmnFile);

        ParsedDataEntity userStory = new ParsedDataEntity(DataType.US, "En tant que manager...");
        userStory.setFile(usFile);

        ParsedDataEntity entity = new ParsedDataEntity(DataType.ENTITY, "absence");
        entity.setFile(mcdFile);

        when(fileRepository.findByVersion(version)).thenReturn(List.of(interviewFile, bpmnFile, usFile, mcdFile));
        when(parsedDataRepository.findByVersion(version))
                .thenReturn(List.of(interviewActor, bpmnActor, userStory, entity));

        when(mistralService.embedParsedData(List.of(interviewActor)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(interviewActor, new float[] { 1.0f, 0.0f })));
        when(mistralService.embedParsedData(List.of(bpmnActor)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(bpmnActor, new float[] { 1.0f, 0.0f })));
        when(mistralService.embedParsedData(List.of(userStory)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(userStory, new float[] { 0.0f, 1.0f })));
        when(mistralService.embedParsedData(List.of(entity)))
                .thenReturn(List.of(new MistralService.EmbeddingTuple(entity, new float[] { 0.5f, 0.5f })));

        when(dataLinkRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<DataLinkEntity> savedLinks = dataLinkService.saveDataLinks(version);

        assertEquals(4, savedLinks.size());
        verify(dataLinkRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }
}
