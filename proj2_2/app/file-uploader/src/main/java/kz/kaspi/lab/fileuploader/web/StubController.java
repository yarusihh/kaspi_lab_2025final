package kz.kaspi.lab.fileuploader.web;

import kz.kaspi.lab.fileuploader.model.FileEntity;
import kz.kaspi.lab.fileuploader.repository.FileRepository;
import kz.kaspi.lab.fileuploader.service.FileDigestService;
import kz.kaspi.lab.fileuploader.service.FileProcessingLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class StubController {

    private static final Logger log = LoggerFactory.getLogger(StubController.class);
    private final FileDigestService fileDigestService;
    private final FileProcessingLockService fileProcessingLockService;
    private final FileRepository fileRepository;

    public StubController(
            FileDigestService fileDigestService,
            FileProcessingLockService fileProcessingLockService,
            FileRepository fileRepository
    ) {
        this.fileDigestService = fileDigestService;
        this.fileProcessingLockService = fileProcessingLockService;
        this.fileRepository = fileRepository;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @ResponseBody
    @GetMapping(value = "/api/v1/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> apiV1Stub() {
        return responseBody(true, "API v1 stub");
    }

    // =========================================================
    // WEBFLUX UPLOAD STUB
    // =========================================================

    @ResponseBody
    @PostMapping(
            value = "/api/v1/upload/",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Map<String, Object>>> uploadStub(
            @RequestPart("file") FilePart file
    ) {

        if (file == null) {
            log.warn("Upload stub: empty file");
            return Mono.just(
                    ResponseEntity.badRequest().body(responseBody(false, "empty file"))
            );
        }

        log.info(
                "Upload accepted: name='{}', headers={}",
                file.filename(),
                file.headers()
        );

        return fileDigestService.digest(file)
                .flatMap(fileDigest -> {
                    String hash = fileDigest.hash();
                    return fileProcessingLockService.markInProgress(hash, file.filename())
                            .flatMap(locked -> {
                                if (!locked) {
                                    return Mono.just(buildAlreadyProcessingResponse());
                                }

                                return saveFileEntity(file, hash, fileDigest.sizeInBytes())
                                        .map(saved -> buildAcceptedResponse())
                                        .onErrorResume(DataIntegrityViolationException.class, ex ->
                                                fileProcessingLockService.removeLock(hash)
                                                        .thenReturn(buildAlreadyExistsInDbResponse()));
                            });
                })
                .onErrorResume(ex -> {
                    log.error("Upload stub failed", ex);
                    return Mono.just(ResponseEntity.internalServerError().body(responseBody(false, "failed to process upload")));
                });
    }

    private ResponseEntity<Map<String, Object>> buildAcceptedResponse() {
        return ResponseEntity.accepted().body(responseBody(true, "file accepted for processing"));
    }

    private ResponseEntity<Map<String, Object>> buildAlreadyProcessingResponse() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(responseBody(false, "file is already in processing"));
    }

    private ResponseEntity<Map<String, Object>> buildAlreadyExistsInDbResponse() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(responseBody(false, "file already exists in database"));
    }

    private FileEntity buildFileEntity(FilePart file, String hash, int sizeInBytes) {
        UUID uuid = UUID.randomUUID();

        return new FileEntity(
                null,
                uuid,
                LocalDateTime.now(),
                file.filename(),
                (long) sizeInBytes,
                hash
        );
    }

    private Mono<FileEntity> saveFileEntity(FilePart file, String hash, int sizeInBytes) {
        FileEntity entity = buildFileEntity(file, hash, sizeInBytes);
        return fileRepository.save(entity);
    }

    private Map<String, Object> responseBody(boolean success, String msg) {
        Map<String, Object> response = new HashMap<>();
        response.put("sucess", success);
        response.put("msg", msg);
        return response;
    }
}
