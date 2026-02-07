package kz.kaspi.lab.fileuploaderreactive.application.port.out;

import java.nio.file.Path;
import reactor.core.publisher.Mono;

public interface FileStorageClient {

    Mono<String> upload(Path tempFile, String fileName, String contentType);

    Mono<Void> delete(String objectKey);
}
