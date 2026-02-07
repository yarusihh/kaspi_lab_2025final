package kz.kaspi.lab.fileuploader.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3UploadService {

    private final S3AsyncClient s3AsyncClient;
    private final String bucket;

    public S3UploadService(
            S3AsyncClient s3AsyncClient,
            @Value("${app.storage.bucket}") String bucket
    ) {
        this.s3AsyncClient = s3AsyncClient;
        this.bucket = bucket;
    }

    public Mono<Void> upload(byte[] content, String objectKey, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();

        return Mono.fromFuture(s3AsyncClient.putObject(request, AsyncRequestBody.fromBytes(content)))
                .then();
    }

    public Mono<Void> delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        return Mono.fromFuture(s3AsyncClient.deleteObject(request)).then();
    }

    public Flux<S3FileInfo> listObjects() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .build();

        return Mono.fromFuture(s3AsyncClient.listObjectsV2(request))
                .flatMapMany(response -> Flux.fromIterable(response.contents()))
                .map(this::toFileInfo);
    }

    private S3FileInfo toFileInfo(S3Object item) {
        return new S3FileInfo(
                item.key(),
                item.size(),
                item.lastModified() == null ? null : item.lastModified().toString()
        );
    }

    public record S3FileInfo(String name, Long size, String uploadDate) {
    }
}
