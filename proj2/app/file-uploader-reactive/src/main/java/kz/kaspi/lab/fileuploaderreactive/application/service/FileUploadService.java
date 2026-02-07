package kz.kaspi.lab.fileuploaderreactive.application.service;

import java.time.Instant;
import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.api.dto.AcceptedUploadResponse;
import kz.kaspi.lab.fileuploaderreactive.application.port.in.FileUploadUseCase;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileMetadataRepository;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.TemporaryFileManager;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileUploadRecord;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.config.UploadProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FileUploadService implements FileUploadUseCase {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    private final UploadProperties uploadProperties;
    private final TemporaryFileManager temporaryFileManager;
    private final UploadAsyncProcessor uploadAsyncProcessor;
    private final FileMetadataRepository fileMetadataRepository;

    public FileUploadService(
            UploadProperties uploadProperties,
            TemporaryFileManager temporaryFileManager,
            UploadAsyncProcessor uploadAsyncProcessor,
            FileMetadataRepository fileMetadataRepository
    ) {
        this.uploadProperties = uploadProperties;
        this.temporaryFileManager = temporaryFileManager;
        this.uploadAsyncProcessor = uploadAsyncProcessor;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @Override
    public Mono<AcceptedUploadResponse> upload(FilePart filePart) {
        String requestId = UUID.randomUUID().toString();
        String contentType = filePart.headers().getContentType() != null
                ? filePart.headers().getContentType().toString()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return temporaryFileManager.createFrom(filePart)
                .doOnNext(tempFile -> {
                    log.info("Request accepted and delegated to async processor: requestId={}, filename={}",
                            requestId, filePart.filename());
                    uploadAsyncProcessor.submit(new UploadTask(requestId, filePart.filename(), contentType, tempFile));
                })
                .map(ignored -> new AcceptedUploadResponse(
                        requestId,
                        "IN_PROCESS",
                        "File accepted. Processing asynchronously.",
                        Instant.now()
                ))
                .timeout(uploadProperties.timeout());
    }

    @Override
    public Mono<FileUploadRecord> getById(String uploadId) {
        return fileMetadataRepository.findById(uploadId);
    }
}
