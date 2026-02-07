package kz.kaspi.lab.fileuploaderreactive.application.port.in;

import kz.kaspi.lab.fileuploaderreactive.api.dto.FileAcceptedResponse;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileRecord;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileUseCase {

    Mono<FileAcceptedResponse> upload(FilePart filePart);

    Flux<FileRecord> getAll();
}
