package kz.kaspi.lab.fileuploader.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
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
}
