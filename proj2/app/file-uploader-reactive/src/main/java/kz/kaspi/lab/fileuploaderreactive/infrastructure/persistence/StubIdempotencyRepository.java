package kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence;

import kz.kaspi.lab.fileuploaderreactive.application.port.out.IdempotencyRepository;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileUploadRecord;
import kz.kaspi.lab.fileuploaderreactive.domain.value.UploadStatus;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.repository.FileUploadR2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StubIdempotencyRepository implements IdempotencyRepository {

    private final FileUploadR2dbcRepository repository;

    public StubIdempotencyRepository(FileUploadR2dbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<FileUploadRecord> findByClientAndKey(String clientId, String idempotencyKey) {
        return repository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey)
                .map(entity -> new FileUploadRecord(
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
                ));
    }

    @Override
    public Mono<Void> reserve(String clientId, String idempotencyKey) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> release(String clientId, String idempotencyKey) {
        return Mono.empty();
    }
}
