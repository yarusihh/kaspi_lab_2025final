package kz.kaspi.lab.fileuploaderreactive.infrastructure.temp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.TempFileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class TempFileStorageAdapter implements TempFileStorage {

    private static final Logger log = LoggerFactory.getLogger(TempFileStorageAdapter.class);

    @Override
    public Mono<Path> createFrom(FilePart filePart) {
        return Mono.fromCallable(() -> Files.createTempFile("upload-", ".tmp"))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(path -> log.info("Temp file created: path={}, filename={}", path, filePart.filename()))
                .flatMap(path -> filePart.transferTo(path)
                        .thenReturn(path)
                        .doOnSuccess(ignored -> log.info("Multipart content transferred to temp file: path={}", path)));
    }

    @Override
    public Mono<Void> cleanup(Path tempFile) {
        return Mono.fromRunnable(() -> {
                    try {
                        Files.deleteIfExists(tempFile);
                        log.info("Temp file cleanup done: path={}", tempFile);
                    } catch (IOException ignored) {
                        log.warn("Temp file cleanup failed: path={}", tempFile);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
