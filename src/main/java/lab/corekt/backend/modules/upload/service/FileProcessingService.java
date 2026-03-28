package lab.corekt.backend.modules.upload.service;

import lab.corekt.backend.modules.mistral.service.MistralService;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.service.BpmnParserService;
import lab.corekt.backend.modules.project.parsing.service.McdParserService;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.enums.ProcessingStatus;
import lab.corekt.backend.modules.upload.event.FileUploadedEvent;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class FileProcessingService {

	private final FileRepository fileRepository;
	private final BpmnParserService bpmnParserService;
	private final McdParserService mcdParserService;
	private final MistralService mistralService;
	private final PlatformTransactionManager transactionManager;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onFileUploaded(FileUploadedEvent event) {
		UUID fileId = event.fileId();
		try {
			runInRequiresNewTransaction(() -> processAndMarkProcessed(fileId));
		} catch (Exception exception) {
			runInRequiresNewTransaction(() -> markFailedAndDelete(fileId));
			log.error("Failed to process file {}", fileId, exception);
		}
	}

	private void processAndMarkProcessed(UUID fileId) {
		fileRepository.findById(fileId).ifPresent(fileEntity -> {
			processFileByType(fileEntity.getType(), fileEntity);
			fileEntity.setProcessingStatus(ProcessingStatus.PROCESSED);
			fileRepository.save(fileEntity);
		});
	}

	private void markFailedAndDelete(UUID fileId) {
		fileRepository.findById(fileId).ifPresent(fileEntity -> {
			fileEntity.setProcessingStatus(ProcessingStatus.FAILED);
			fileRepository.save(fileEntity);
			fileRepository.delete(fileEntity);
		});
	}

	private void runInRequiresNewTransaction(Runnable action) {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.executeWithoutResult(status -> action.run());
	}

	protected void processFileByType(@NonNull FileType fileType, FileEntity fileEntity) {
		try {
			switch (fileType) {
				case BPMN -> bpmnParserService.processFile(fileEntity);
				case MCD -> mcdParserService.processFile(fileEntity);
				case INTERVIEW -> mistralService.processInterview(fileEntity);
				default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type: " + fileType);
			}
		} catch (Exception exception) {
			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to parse the uploaded file: " + exception.getMessage(),
					exception
			);
		}
	}
}


