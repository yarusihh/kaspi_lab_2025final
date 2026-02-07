package kz.kaspi.lab.fileuploader.repository;

import kz.kaspi.lab.fileuploader.model.FileEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface FileRepository extends ReactiveCrudRepository<FileEntity, Long> {

    Mono<FileEntity> findByUuid(UUID uuid);

    Mono<FileEntity> findByHash(String hash);

    Mono<Boolean> existsByHash(String hash);
}
