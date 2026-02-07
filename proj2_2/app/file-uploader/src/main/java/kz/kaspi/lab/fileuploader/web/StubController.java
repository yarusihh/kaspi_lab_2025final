package kz.kaspi.lab.fileuploader.web;

import kz.kaspi.lab.fileuploader.service.FileUploadProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Controller
public class StubController {

    private static final Logger log = LoggerFactory.getLogger(StubController.class);
    private final FileUploadProcessingService fileUploadProcessingService;

    public StubController(FileUploadProcessingService fileUploadProcessingService) {
        this.fileUploadProcessingService = fileUploadProcessingService;
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

        return fileUploadProcessingService.process(file)
                .map(this::toHttpResponse)
                .onErrorResume(ex -> {
                    log.error("Upload stub failed", ex);
                    return Mono.just(ResponseEntity.internalServerError().body(responseBody(false, "failed to process upload")));
                });
    }

    private ResponseEntity<Map<String, Object>> buildAcceptedResponse() {
        return ResponseEntity.accepted().body(responseBody(true, "file accepted for processing"));
    }

    private ResponseEntity<Map<String, Object>> toHttpResponse(FileUploadProcessingService.UploadStatus status) {
        return switch (status) {
            case ACCEPTED -> buildAcceptedResponse();
            case ALREADY_PROCESSING -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(responseBody(false, "file is already in processing"));
            case ALREADY_EXISTS_IN_DB -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(responseBody(false, "file already exists in database"));
        };
    }

    private Map<String, Object> responseBody(boolean success, String msg) {
        Map<String, Object> response = new HashMap<>();
        response.put("sucess", success);
        response.put("msg", msg);
        return response;
    }
}
