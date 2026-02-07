package kz.kaspi.lab.fileuploader.service;

import kz.kaspi.lab.fileuploader.model.FileEntity;
import kz.kaspi.lab.fileuploader.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileUploadProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadProcessingService.class);

    private final FileDigestService fileDigestService;
    private final FileProcessingLockService fileProcessingLockService;
    private final FileRepository fileRepository;
    private final S3UploadService s3UploadService;

    public FileUploadProcessingService(
            FileDigestService fileDigestService,
            FileProcessingLockService fileProcessingLockService,
            FileRepository fileRepository,
            S3UploadService s3UploadService
    ) {
        this.fileDigestService = fileDigestService;
        this.fileProcessingLockService = fileProcessingLockService;
        this.fileRepository = fileRepository;
        this.s3UploadService = s3UploadService;
    }

    public Mono<UploadStatus> process(FilePart file) {
        return fileDigestService.digest(file)
                .flatMap(fileDigest -> {
                    String hash = fileDigest.hash();
                    return fileProcessingLockService.markInProgress(hash, file.filename())
                            .flatMap(locked -> {
                                if (!locked) {
                                    return Mono.just(UploadStatus.ALREADY_PROCESSING);
                                }

                                return saveFileEntity(file, hash, fileDigest.sizeInBytes())
                                        .doOnSuccess(saved -> startAsyncUpload(file, fileDigest))
                                        .thenReturn(UploadStatus.ACCEPTED)
                                        .onErrorResume(DataIntegrityViolationException.class, ex ->
                                                fileProcessingLockService.removeLock(hash)
                                                        .thenReturn(UploadStatus.ALREADY_EXISTS_IN_DB));
                            });
                });
    }

    private void startAsyncUpload(FilePart file, FileDigestService.FileDigest fileDigest) {
        String contentType = file.headers().getContentType() == null
                ? "application/octet-stream"
                : file.headers().getContentType().toString();
        String objectKey = file.filename();

        s3UploadService.upload(fileDigest.bytes(), objectKey, contentType)
                .doOnSuccess(unused -> log.info("S3 upload completed: key={}, hash={}", objectKey, fileDigest.hash()))
                .doOnError(error -> log.error("S3 upload failed: key={}, hash={}", objectKey, fileDigest.hash(), error))
                .onErrorResume(error -> Mono.empty())
                .doFinally(signal -> fileProcessingLockService.removeLock(fileDigest.hash()).subscribe())
                .subscribe();
    }

    private Mono<FileEntity> saveFileEntity(FilePart file, String hash, int sizeInBytes) {
        UUID uuid = UUID.randomUUID();
        FileEntity entity = new FileEntity(
                null,
                uuid,
                LocalDateTime.now(),
                file.filename(),
                (long) sizeInBytes,
                hash
        );
        return fileRepository.save(entity);
    }

    public enum UploadStatus {
        ACCEPTED,
        ALREADY_PROCESSING,
        ALREADY_EXISTS_IN_DB
    }
}
