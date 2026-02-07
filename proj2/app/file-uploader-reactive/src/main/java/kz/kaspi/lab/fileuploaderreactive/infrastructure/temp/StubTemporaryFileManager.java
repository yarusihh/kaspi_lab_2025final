package kz.kaspi.lab.fileuploaderreactive.infrastructure.temp;

import java.nio.file.Path;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.TemporaryFileManager;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StubTemporaryFileManager implements TemporaryFileManager {

    @Override
    public Mono<Path> createFrom(FilePart filePart) {
        return Mono.error(new UnsupportedOperationException("TODO: implement temp file creation"));
    }

    @Override
    public Mono<Void> cleanup(Path tempFile) {
        return Mono.empty();
    }
}
