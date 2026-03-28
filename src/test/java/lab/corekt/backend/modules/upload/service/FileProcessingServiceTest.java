package lab.corekt.backend.modules.upload.service;

import lab.corekt.backend.modules.mistral.service.MistralService;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.service.BpmnParserService;
import lab.corekt.backend.modules.project.parsing.service.McdParserService;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.enums.ProcessingStatus;
import lab.corekt.backend.modules.upload.event.FileUploadedEvent;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileProcessingServiceTest {

	@Mock
	private FileRepository fileRepository;

	@Mock
	private BpmnParserService bpmnParserService;

	@Mock
	private McdParserService mcdParserService;

	@Mock
	private MistralService mistralService;

	@InjectMocks
	private FileProcessingService fileProcessingService;

	@Test
	@DisplayName("onFileUploaded processes BPMN file and marks it as PROCESSED")
	void onFileUploadedProcessesBpmnFile() throws Exception {
		UUID fileId = UUID.randomUUID();
		FileEntity fileEntity = new FileEntity(FileType.BPMN, "diagram.bpmn");
		fileEntity.setId(fileId);

		when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileEntity));
		when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		fileProcessingService.onFileUploaded(new FileUploadedEvent(fileId));

		verify(bpmnParserService).processFile(fileEntity);
		verify(mcdParserService, never()).processFile(any());
		verify(mistralService, never()).processInterview(any());

		ArgumentCaptor<FileEntity> captor = ArgumentCaptor.forClass(FileEntity.class);
		verify(fileRepository).save(captor.capture());
		assertEquals(ProcessingStatus.PROCESSED, captor.getValue().getProcessingStatus());
	}

	@Test
	@DisplayName("onFileUploaded processes MCD file and marks it as PROCESSED")
	void onFileUploadedProcessesMcdFile() throws Exception {
		UUID fileId = UUID.randomUUID();
		FileEntity fileEntity = new FileEntity(FileType.MCD, "diagram.mcd");
		fileEntity.setId(fileId);

		when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileEntity));
		when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		fileProcessingService.onFileUploaded(new FileUploadedEvent(fileId));

		verify(mcdParserService).processFile(fileEntity);
		verify(bpmnParserService, never()).processFile(any());
		verify(mistralService, never()).processInterview(any());
		assertEquals(ProcessingStatus.PROCESSED, fileEntity.getProcessingStatus());
	}

	@Test
	@DisplayName("onFileUploaded processes INTERVIEW file and marks it as PROCESSED")
	void onFileUploadedProcessesInterviewFile() throws Exception {
		UUID fileId = UUID.randomUUID();
		FileEntity fileEntity = new FileEntity(FileType.INTERVIEW, "interview.pdf");
		fileEntity.setId(fileId);

		when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileEntity));
		when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		fileProcessingService.onFileUploaded(new FileUploadedEvent(fileId));

		verify(mistralService).processInterview(fileEntity);
		verify(bpmnParserService, never()).processFile(any());
		verify(mcdParserService, never()).processFile(any());
		assertEquals(ProcessingStatus.PROCESSED, fileEntity.getProcessingStatus());
	}

	@Test
	@DisplayName("onFileUploaded marks file FAILED and deletes it when processing throws")
	void onFileUploadedMarksFailureAndDeletesFile() throws Exception {
		UUID fileId = UUID.randomUUID();
		FileEntity fileEntity = new FileEntity(FileType.BPMN, "diagram.bpmn");
		fileEntity.setId(fileId);

		when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileEntity));
		doThrow(new IllegalStateException("invalid bpmn")).when(bpmnParserService).processFile(fileEntity);
		when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		fileProcessingService.onFileUploaded(new FileUploadedEvent(fileId));

		assertEquals(ProcessingStatus.FAILED, fileEntity.getProcessingStatus());
		verify(fileRepository).save(fileEntity);
		verify(fileRepository).delete(fileEntity);
	}

	@Test
	@DisplayName("onFileUploaded does nothing when uploaded file no longer exists")
	void onFileUploadedDoesNothingWhenMissingFile() {
		UUID fileId = UUID.randomUUID();

		when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

		fileProcessingService.onFileUploaded(new FileUploadedEvent(fileId));

		verifyNoInteractions(bpmnParserService, mcdParserService, mistralService);
		verify(fileRepository, never()).save(any());
		verify(fileRepository, never()).delete(any());
	}
}


