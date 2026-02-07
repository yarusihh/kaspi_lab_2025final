package kz.kaspi.lab.fileuploader.web;

import kz.kaspi.lab.fileuploader.model.FileEntity;
import kz.kaspi.lab.fileuploader.repository.FileRepository;
import kz.kaspi.lab.fileuploader.service.FileUploadProcessingService;
import kz.kaspi.lab.fileuploader.service.S3UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
public class StubController {

    private static final Logger log = LoggerFactory.getLogger(StubController.class);

    private final FileUploadProcessingService fileUploadProcessingService;
    private final S3UploadService s3UploadService;
    private final FileRepository fileRepository;
    private final String storageBrowserBaseUrl;

    public StubController(
            FileUploadProcessingService fileUploadProcessingService,
            S3UploadService s3UploadService,
            FileRepository fileRepository,
            @Value("${app.storage.browser-base-url:https://minio-ui.icod.kz/browser/kaspi-lab-public/}") String storageBrowserBaseUrl
    ) {
        this.fileUploadProcessingService = fileUploadProcessingService;
        this.s3UploadService = s3UploadService;
        this.fileRepository = fileRepository;
        this.storageBrowserBaseUrl = storageBrowserBaseUrl;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @ResponseBody
    @GetMapping(value = "/api/v1/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> apiV1Stub() {
        return responseBody(true, "API v1 работает.");
    }

    @ResponseBody
    @GetMapping(value = "/api/v1/storage/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> listS3Files() {
        return s3UploadService.listObjects()
                .take(20)
                .map(this::s3FileDescriptor)
                .collectList()
                .map(files -> {
                    Map<String, Object> response = responseBody(true, "Список файлов из S3 успешно получен (максимум 20).");
                    response.put("files", files);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(ex -> {
                    log.error("Failed to list S3 files", ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            responseBody(false, "Не удалось получить список файлов из S3: " + safeErrorMessage(ex))
                    ));
                });
    }

    @ResponseBody
    @GetMapping(value = "/api/v1/db/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> listDbFiles() {
        return fileRepository.findAll()
                .take(20)
                .map(this::dbFileDescriptor)
                .collectList()
                .map(files -> {
                    Map<String, Object> response = responseBody(true, "Список файлов из базы данных успешно получен (максимум 20).");
                    response.put("files", files);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(ex -> {
                    log.error("Failed to list DB files", ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            responseBody(false, "Не удалось получить список файлов из базы данных: " + safeErrorMessage(ex))
                    ));
                });
    }

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
            return Mono.just(ResponseEntity.badRequest().body(responseBody(false, "Файл не передан в запросе.")));
        }

        log.info("Upload accepted: name='{}', headers={}", file.filename(), file.headers());

        return fileUploadProcessingService.process(file)
                .map(this::toHttpResponse)
                .onErrorResume(ex -> {
                    log.error("Upload stub failed", ex);
                    return Mono.just(ResponseEntity.internalServerError().body(
                            responseBody(false, "Внутренняя ошибка контроллера: " + safeErrorMessage(ex))
                    ));
                });
    }

    private ResponseEntity<Map<String, Object>> toHttpResponse(FileUploadProcessingService.UploadStatus status) {
        return switch (status) {
            case ACCEPTED -> ResponseEntity.accepted().body(responseBody(true, "Файл принят в обработку."));
            case ALREADY_PROCESSING -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(responseBody(false, "Файл с таким содержимым уже обрабатывается."));
            case ALREADY_EXISTS_IN_DB -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(responseBody(false, "Файл с таким hash уже существует в базе данных."));
        };
    }

    private Map<String, Object> s3FileDescriptor(S3UploadService.S3FileInfo fileInfo) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", null);
        item.put("uuid", null);
        item.put("uploadDate", fileInfo.uploadDate());
        item.put("name", fileInfo.name());
        item.put("size", fileInfo.size());
        item.put("hash", null);
        item.put("url", buildStorageBrowserLink(fileInfo.name()));
        return item;
    }

    private Map<String, Object> dbFileDescriptor(FileEntity fileEntity) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", fileEntity.getId());
        item.put("uuid", fileEntity.getUuid());
        item.put("uploadDate", fileEntity.getUploadDate());
        item.put("name", fileEntity.getName());
        item.put("size", fileEntity.getSize());
        item.put("hash", fileEntity.getHash());
        item.put("url", buildStorageBrowserLink(fileEntity.getName()));
        return item;
    }

    private String buildStorageBrowserLink(String fileName) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String base = storageBrowserBaseUrl.endsWith("/") ? storageBrowserBaseUrl : storageBrowserBaseUrl + "/";
        return base + encodedFileName;
    }

    private Map<String, Object> responseBody(boolean success, String msg) {
        Map<String, Object> response = new HashMap<>();
        response.put("sucess", success);
        response.put("success", success);
        response.put("msg", msg);
        return response;
    }

    private String safeErrorMessage(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return "причина не указана";
        }
        return error.getMessage();
    }
}