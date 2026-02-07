package kz.kaspi.lab.fileuploader.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
public class StubController {

    private static final Logger log = LoggerFactory.getLogger(StubController.class);

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @ResponseBody
    @GetMapping(value = "/api/v1/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> apiV1Stub() {
        return Map.of(
                "status", "ok",
                "message", "API v1 stub"
        );
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
                    ResponseEntity.badRequest().body(Map.of(
                            "status", "error",
                            "message", "empty file"
                    ))
            );
        }

        log.info(
                "Upload accepted: name='{}', headers={}",
                file.filename(),
                file.headers()
        );

        return Mono.just(
                ResponseEntity.accepted().body(Map.of(
                        "status", "success",
                        "message", "file accepted for processing"
                ))
        );
    }
}
