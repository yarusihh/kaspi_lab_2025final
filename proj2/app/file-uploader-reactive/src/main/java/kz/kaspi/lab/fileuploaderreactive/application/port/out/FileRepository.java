package kz.kaspi.lab.fileuploaderreactive.application.port.out;

import kz.kaspi.lab.fileuploaderreactive.domain.model.FileRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileRepository {

    Mono<FileRecord> save(FileRecord record);

    Mono<FileRecord> findById(String id);

    Flux<FileRecord> findAll();

    Mono<Void> deleteById(String id);
}
