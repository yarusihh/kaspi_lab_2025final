package kz.kaspi.lab.fileuploaderreactive.application.port.out;

import kz.kaspi.lab.fileuploaderreactive.domain.model.FileUploadRecord;
import reactor.core.publisher.Mono;

public interface FileMetadataRepository {

    Mono<FileUploadRecord> save(FileUploadRecord record);

    Mono<FileUploadRecord> findById(String uploadId);
}
