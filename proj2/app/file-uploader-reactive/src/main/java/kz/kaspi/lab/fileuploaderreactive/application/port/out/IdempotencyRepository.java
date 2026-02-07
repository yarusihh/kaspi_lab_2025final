package kz.kaspi.lab.fileuploaderreactive.application.port.out;

import kz.kaspi.lab.fileuploaderreactive.domain.model.FileUploadRecord;
import reactor.core.publisher.Mono;

public interface IdempotencyRepository {

    Mono<FileUploadRecord> findByClientAndKey(String clientId, String idempotencyKey);

    Mono<Void> reserve(String clientId, String idempotencyKey);

    Mono<Void> release(String clientId, String idempotencyKey);
}
