package kz.kaspi.lab.fileuploaderreactive.application.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.application.port.in.FileUploadUseCase;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileMetadataRepository;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileStorageClient;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.IdempotencyRepository;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.TemporaryFileManager;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileUploadRecord;
import kz.kaspi.lab.fileuploaderreactive.domain.value.UploadStatus;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.config.UploadProperties;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FileUploadService implements FileUploadUseCase {

    private final IdempotencyRepository idempotencyRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageClient fileStorageClient;
    private final TemporaryFileManager temporaryFileManager;
    private final UploadProperties uploadProperties;

    public FileUploadService(
            IdempotencyRepository idempotencyRepository,
            FileMetadataRepository fileMetadataRepository,
            FileStorageClient fileStorageClient,
            TemporaryFileManager temporaryFileManager,
            UploadProperties uploadProperties
    ) {
        this.idempotencyRepository = idempotencyRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileStorageClient = fileStorageClient;
        this.temporaryFileManager = temporaryFileManager;
        this.uploadProperties = uploadProperties;
    }

    @Override
    public Mono<FileUploadRecord> upload(String clientId, String idempotencyKey, FilePart filePart) {
        return idempotencyRepository.findByClientAndKey(clientId, idempotencyKey)
                .switchIfEmpty(processNewUpload(clientId, idempotencyKey, filePart))
                .timeout(uploadProperties.timeout());
    }

    @Override
    public Mono<FileUploadRecord> getById(String uploadId) {
        return fileMetadataRepository.findById(uploadId);
    }

    private Mono<FileUploadRecord> processNewUpload(String clientId, String idempotencyKey, FilePart filePart) {
        return idempotencyRepository.reserve(clientId, idempotencyKey)
                .then(temporaryFileManager.createFrom(filePart))
                .flatMap(tempFile -> uploadAndPersist(clientId, idempotencyKey, filePart, tempFile)
                        .flatMap(result -> temporaryFileManager.cleanup(tempFile).thenReturn(result))
                        .onErrorResume(error -> temporaryFileManager.cleanup(tempFile).then(Mono.error(error))))
                .onErrorResume(error -> idempotencyRepository.release(clientId, idempotencyKey).then(Mono.error(error)));
    }

    private Mono<FileUploadRecord> uploadAndPersist(
            String clientId,
            String idempotencyKey,
            FilePart filePart,
            Path tempFile
    ) {
        String contentType = filePart.headers().getContentType() != null
                ? filePart.headers().getContentType().toString()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return fileStorageClient.upload(tempFile, filePart.filename(), contentType)
                .flatMap(objectKey -> {
                    FileUploadRecord record = new FileUploadRecord(
                            UUID.randomUUID(),
                            clientId,
                            idempotencyKey,
                            filePart.filename(),
                            contentType,
                            -1L,
                            objectKey,
                            UploadStatus.COMPLETED,
                            Instant.now(),
                            Instant.now()
                    );
                    return fileMetadataRepository.save(record);
                });
    }
}
