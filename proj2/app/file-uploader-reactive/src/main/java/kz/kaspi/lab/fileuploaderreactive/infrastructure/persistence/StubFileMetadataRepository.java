package kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence;

import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileMetadataRepository;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileUploadRecord;
import kz.kaspi.lab.fileuploaderreactive.domain.value.UploadStatus;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.entity.FileUploadEntity;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.repository.FileUploadR2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StubFileMetadataRepository implements FileMetadataRepository {

    private final FileUploadR2dbcRepository repository;

    public StubFileMetadataRepository(FileUploadR2dbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<FileUploadRecord> save(FileUploadRecord record) {
        return repository.save(toEntity(record)).map(this::toDomain);
    }

    @Override
    public Mono<FileUploadRecord> findById(String uploadId) {
        return Mono.fromCallable(() -> UUID.fromString(uploadId))
                .flatMap(repository::findById)
                .map(this::toDomain);
    }

    private FileUploadEntity toEntity(FileUploadRecord record) {
        FileUploadEntity entity = new FileUploadEntity();
        entity.setId(record.id());
        entity.setClientId(record.clientId());
        entity.setIdempotencyKey(record.idempotencyKey());
        entity.setFileName(record.fileName());
        entity.setContentType(record.contentType());
        entity.setSizeBytes(record.sizeBytes());
        entity.setStorageObjectKey(record.storageObjectKey());
        entity.setStatus(record.status().name());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    private FileUploadRecord toDomain(FileUploadEntity entity) {
        return new FileUploadRecord(
                entity.getId(),
                entity.getClientId(),
                entity.getIdempotencyKey(),
                entity.getFileName(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getStorageObjectKey(),
                UploadStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
