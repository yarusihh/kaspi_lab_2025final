package kz.kaspi.lab.fileuploaderreactive.application.service;

import java.time.Instant;
import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.api.dto.FileAcceptedResponse;
import kz.kaspi.lab.fileuploaderreactive.application.port.in.FileUseCase;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileRepository;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.TempFileStorage;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileRecord;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.config.FileUploadProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class FileService implements FileUseCase {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileUploadProperties fileUploadProperties;
    private final TempFileStorage tempFileStorage;
    private final FileProcessingWorker fileProcessingWorker;
    private final FileRepository fileRepository;

    public FileService(
            FileUploadProperties fileUploadProperties,
            TempFileStorage tempFileStorage,
            FileProcessingWorker fileProcessingWorker,
            FileRepository fileRepository
    ) {
        this.fileUploadProperties = fileUploadProperties;
        this.tempFileStorage = tempFileStorage;
        this.fileProcessingWorker = fileProcessingWorker;
        this.fileRepository = fileRepository;
    }

    @Override
    public Mono<FileAcceptedResponse> upload(FilePart filePart) {
        String requestId = UUID.randomUUID().toString();
        String contentType = filePart.headers().getContentType() != null
                ? filePart.headers().getContentType().toString()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return tempFileStorage.createFrom(filePart)
                .doOnNext(tempFile -> {
                    log.info("Request accepted and delegated to worker: requestId={}, filename={}",
                            requestId, filePart.filename());
                    fileProcessingWorker.submit(new FileProcessingTask(requestId, filePart.filename(), contentType, tempFile));
                })
                .map(ignored -> new FileAcceptedResponse(
                        requestId,
                        "IN_PROCESS",
                        "File accepted. Processing asynchronously.",
                        Instant.now()
                ))
                .timeout(fileUploadProperties.timeout());
    }

    @Override
    public Flux<FileRecord> getAll() {
        return fileRepository.findAll();
    }
}
