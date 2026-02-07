package kz.kaspi.lab.fileuploaderreactive.infrastructure.storage;

import java.nio.file.Path;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileStorageClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StubStorageClient implements FileStorageClient {

    @Override
    public Mono<String> upload(Path tempFile, String fileName, String contentType) {
        return Mono.error(new UnsupportedOperationException("TODO: implement external storage upload adapter"));
    }

    @Override
    public Mono<Void> delete(String objectKey) {
        return Mono.error(new UnsupportedOperationException("TODO: implement external storage delete adapter"));
    }
}
