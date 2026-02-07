package kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence;

import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileMetadataRepository;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileUploadRecord;
import kz.kaspi.lab.fileuploaderreactive.domain.value.UploadStatus;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.entity.FileUploadEntity;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.repository.FileUploadR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StubFileMetadataRepository implements FileMetadataRepository {

    private static final Logger log = LoggerFactory.getLogger(StubFileMetadataRepository.class);

    private final FileUploadR2dbcRepository repository;

    public StubFileMetadataRepository(FileUploadR2dbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<FileUploadRecord> save(FileUploadRecord record) {
        log.info("DB save started: uploadId={}, checksum={}, status={}", record.id(), record.checksum(), record.status());
        return repository.save(toEntity(record))
                .map(this::toDomain)
                .doOnSuccess(saved -> log.info("DB save succeeded: uploadId={}", saved.id()))
                .doOnError(error -> log.error("DB save failed: uploadId={}", record.id(), error));
    }

    @Override
    public Mono<FileUploadRecord> findById(String uploadId) {
        log.info("DB findById started: uploadId={}", uploadId);
        return Mono.fromCallable(() -> UUID.fromString(uploadId))
                .flatMap(repository::findById)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(String uploadId) {
        log.info("DB deleteById started: uploadId={}", uploadId);
        return Mono.fromCallable(() -> UUID.fromString(uploadId))
                .flatMap(repository::deleteById)
                .doOnSuccess(ignored -> log.info("DB deleteById succeeded: uploadId={}", uploadId))
                .doOnError(error -> log.error("DB deleteById failed: uploadId={}", uploadId, error));
    }

    private FileUploadEntity toEntity(FileUploadRecord record) {
        FileUploadEntity entity = new FileUploadEntity();
        entity.setId(record.id());
        entity.setChecksum(record.checksum());
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
                entity.getChecksum(),
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
