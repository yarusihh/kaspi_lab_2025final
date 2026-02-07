package kz.kaspi.lab.fileuploaderreactive.infrastructure.storage;

import java.nio.file.Path;
import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileStorageClient;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class StubStorageClient implements FileStorageClient {

    private static final Logger log = LoggerFactory.getLogger(StubStorageClient.class);

    private final S3AsyncClient s3AsyncClient;
    private final StorageProperties storageProperties;

    public StubStorageClient(S3AsyncClient s3AsyncClient, StorageProperties storageProperties) {
        this.s3AsyncClient = s3AsyncClient;
        this.storageProperties = storageProperties;
    }

    @Override
    public Mono<String> upload(Path tempFile, String fileName, String contentType) {
        String objectKey = "uploads/" + UUID.randomUUID() + "-" + sanitizeFileName(fileName);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        log.info("S3 upload started: bucket={}, objectKey={}, file={}", storageProperties.bucket(), objectKey, tempFile);
        return Mono.fromFuture(s3AsyncClient.putObject(request, AsyncRequestBody.fromFile(tempFile)))
                .doOnSuccess(ignored -> log.info("S3 upload succeeded: objectKey={}", objectKey))
                .doOnError(error -> log.error("S3 upload failed: objectKey={}", objectKey, error))
                .thenReturn(objectKey);
    }

    @Override
    public Mono<Void> delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(objectKey)
                .build();

        log.info("S3 delete started: bucket={}, objectKey={}", storageProperties.bucket(), objectKey);
        return Mono.fromFuture(s3AsyncClient.deleteObject(request))
                .doOnSuccess(ignored -> log.info("S3 delete succeeded: objectKey={}", objectKey))
                .doOnError(error -> log.error("S3 delete failed: objectKey={}", objectKey, error))
                .then();
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replace("\\", "_").replace("/", "_").replace(" ", "_");
    }
}
