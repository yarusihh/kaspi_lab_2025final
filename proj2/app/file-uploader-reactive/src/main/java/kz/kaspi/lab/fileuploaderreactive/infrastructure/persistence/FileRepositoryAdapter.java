package kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence;

import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileRepository;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileRecord;
import kz.kaspi.lab.fileuploaderreactive.domain.value.FileStatus;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.entity.FileRecordEntity;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.repository.FileRecordR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class FileRepositoryAdapter implements FileRepository {

    private static final Logger log = LoggerFactory.getLogger(FileRepositoryAdapter.class);

    private final FileRecordR2dbcRepository repository;

    public FileRepositoryAdapter(FileRecordR2dbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<FileRecord> save(FileRecord record) {
        log.info("DB save started: id={}, checksum={}, status={}", record.id(), record.checksum(), record.status());
        return repository.save(toEntity(record))
                .map(this::toDomain)
                .doOnSuccess(saved -> log.info("DB save succeeded: id={}", saved.id()))
                .doOnError(error -> log.error("DB save failed: id={}", record.id(), error));
    }

    @Override
    public Mono<FileRecord> findById(String id) {
        return Mono.fromCallable(() -> UUID.fromString(id)).flatMap(repository::findById).map(this::toDomain);
    }

    @Override
    public Flux<FileRecord> findAll() {
        return repository.findAll().map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return Mono.fromCallable(() -> UUID.fromString(id)).flatMap(repository::deleteById);
    }

    private FileRecordEntity toEntity(FileRecord record) {
        FileRecordEntity entity = new FileRecordEntity();
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

    private FileRecord toDomain(FileRecordEntity entity) {
        return new FileRecord(
                entity.getId(),
                entity.getChecksum(),
                entity.getFileName(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getStorageObjectKey(),
                FileStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
