package kz.kaspi.lab.fileuploaderreactive.application.port.out;

import java.nio.file.Path;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface TemporaryFileManager {

    Mono<Path> createFrom(FilePart filePart);

    Mono<Void> cleanup(Path tempFile);
}
