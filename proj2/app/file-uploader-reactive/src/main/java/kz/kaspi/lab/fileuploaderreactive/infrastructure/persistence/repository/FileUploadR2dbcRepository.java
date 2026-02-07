package kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.repository;

import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.entity.FileUploadEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface FileUploadR2dbcRepository extends ReactiveCrudRepository<FileUploadEntity, UUID> {

    Mono<FileUploadEntity> findByClientIdAndIdempotencyKey(String clientId, String idempotencyKey);
}
