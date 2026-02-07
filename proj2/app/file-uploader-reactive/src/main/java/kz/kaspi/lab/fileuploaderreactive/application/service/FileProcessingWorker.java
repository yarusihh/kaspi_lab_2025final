package kz.kaspi.lab.fileuploaderreactive.application.service;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileRepository;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.FileStorageClient;
import kz.kaspi.lab.fileuploaderreactive.application.port.out.TempFileStorage;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileRecord;
import kz.kaspi.lab.fileuploaderreactive.domain.value.FileStatus;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.config.DedupProperties;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.dedup.RecentChecksumCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class FileProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingWorker.class);
    private static final String REDIS_KEY_PREFIX = "upload:checksum:";

    private final FileRepository fileRepository;
    private final FileStorageClient fileStorageClient;
    private final TempFileStorage tempFileStorage;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final RecentChecksumCache recentChecksumCache;
    private final DedupProperties dedupProperties;

    public FileProcessingWorker(
            FileRepository fileRepository,
            FileStorageClient fileStorageClient,
            TempFileStorage tempFileStorage,
            ReactiveStringRedisTemplate redisTemplate,
            RecentChecksumCache recentChecksumCache,
            DedupProperties dedupProperties
    ) {
        this.fileRepository = fileRepository;
        this.fileStorageClient = fileStorageClient;
        this.tempFileStorage = tempFileStorage;
        this.redisTemplate = redisTemplate;
        this.recentChecksumCache = recentChecksumCache;
        this.dedupProperties = dedupProperties;
    }

    public void submit(FileProcessingTask task) {
        process(task)
                .doFinally(signal -> tempFileStorage.cleanup(task.tempFile()).subscribe())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> {
                    log.error("Async task failed: requestId={}", task.requestId(), error);
                    return Mono.empty();
                })
                .subscribe();
    }

    private Mono<Void> process(FileProcessingTask task) {
        log.info("Async task started: requestId={}, filename={}", task.requestId(), task.fileName());
        return calculateChecksum(task)
                .flatMap(checksum -> isDuplicate(checksum)
                        .flatMap(duplicate -> {
                            if (duplicate) {
                                log.info("Duplicate detected, skipping storage/db: requestId={}, checksum={}",
                                        task.requestId(), checksum);
                                return Mono.empty();
                            }
                            return persistAndUpload(task, checksum);
                        }))
                .doOnSuccess(ignored -> log.info("Async task finished: requestId={}", task.requestId()));
    }

    private Mono<String> calculateChecksum(FileProcessingTask task) {
        return Mono.fromCallable(() -> {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    try (InputStream stream = Files.newInputStream(task.tempFile())) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = stream.read(buffer)) > 0) {
                            digest.update(buffer, 0, read);
                        }
                    }
                    return String.format("%064x", new BigInteger(1, digest.digest()));
                })
                .doOnNext(checksum -> log.info("Checksum computed: requestId={}, checksum={}", task.requestId(), checksum));
    }

    private Mono<Boolean> isDuplicate(String checksum) {
        if (recentChecksumCache.contains(checksum)) {
            log.info("Local dedup hit: checksum={}", checksum);
            return Mono.just(true);
        }

        return redisTemplate.opsForValue().get(redisKey(checksum))
                .map(value -> true)
                .defaultIfEmpty(false)
                .doOnNext(hit -> log.info("Redis dedup {}: checksum={}", hit ? "hit" : "miss", checksum))
                .onErrorResume(error -> {
                    log.warn("Redis check failed, continue as non-duplicate: checksum={}", checksum, error);
                    return Mono.just(false);
                });
    }

    private Mono<Void> persistAndUpload(FileProcessingTask task, String checksum) {
        String requestId = task.requestId();
        FileRecord pendingRecord = new FileRecord(
                UUID.fromString(requestId),
                checksum,
                task.fileName(),
                task.contentType(),
                -1L,
                "",
                FileStatus.PENDING,
                Instant.now(),
                Instant.now()
        );

        return fileRepository.save(pendingRecord)
                .flatMap(savedPending -> fileStorageClient.upload(task.tempFile(), task.fileName(), task.contentType())
                        .flatMap(objectKey -> {
                            FileRecord completedRecord = new FileRecord(
                                    savedPending.id(),
                                    checksum,
                                    savedPending.fileName(),
                                    savedPending.contentType(),
                                    savedPending.sizeBytes(),
                                    objectKey,
                                    FileStatus.COMPLETED,
                                    savedPending.createdAt(),
                                    Instant.now()
                            );

                            return fileRepository.save(completedRecord)
                                    .flatMap(saved -> markAsRecent(saved.checksum(), saved.id().toString()));
                        })
                        .onErrorResume(uploadError -> rollbackDbAndS3(savedPending.id().toString(), uploadError))
                );
    }

    private Mono<Void> markAsRecent(String checksum, String uploadId) {
        recentChecksumCache.put(checksum, dedupProperties.localTtl());
        return redisTemplate.opsForValue()
                .set(redisKey(checksum), uploadId, dedupProperties.redisTtl())
                .doOnError(error -> log.warn("Redis set failed: checksum={}", checksum, error))
                .onErrorResume(error -> Mono.just(false))
                .then();
    }

    private Mono<Void> rollbackDbAndS3(String uploadId, Throwable uploadError) {
        log.error("Upload failed, rollback DB record: uploadId={}", uploadId, uploadError);
        return fileRepository.findById(uploadId)
                .flatMap(record -> {
                    Mono<Void> rollbackS3 = record.storageObjectKey() == null || record.storageObjectKey().isBlank()
                            ? Mono.empty()
                            : fileStorageClient.delete(record.storageObjectKey())
                            .doOnError(error -> log.error("S3 rollback delete failed: uploadId={}", uploadId, error))
                            .onErrorResume(error -> Mono.empty());

                    return rollbackS3.then(fileRepository.deleteById(uploadId));
                })
                .switchIfEmpty(fileRepository.deleteById(uploadId))
                .then();
    }

    private String redisKey(String checksum) {
        return REDIS_KEY_PREFIX + checksum;
    }
}
