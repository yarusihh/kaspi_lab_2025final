package kz.kaspi.lab.fileuploaderreactive.application.port.in;

import kz.kaspi.lab.fileuploaderreactive.domain.model.FileUploadRecord;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface FileUploadUseCase {

    Mono<FileUploadRecord> upload(String clientId, String idempotencyKey, FilePart filePart);

    Mono<FileUploadRecord> getById(String uploadId);
}
