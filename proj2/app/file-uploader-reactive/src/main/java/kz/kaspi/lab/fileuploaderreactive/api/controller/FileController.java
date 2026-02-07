package kz.kaspi.lab.fileuploaderreactive.api.controller;

import kz.kaspi.lab.fileuploaderreactive.api.dto.FileAcceptedResponse;
import kz.kaspi.lab.fileuploaderreactive.api.dto.FileResponse;
import kz.kaspi.lab.fileuploaderreactive.application.port.in.FileUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/v1/uploads")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileUseCase fileUseCase;

    public FileController(FileUseCase fileUseCase) {
        this.fileUseCase = fileUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<FileAcceptedResponse>> upload(@RequestPart("file") FilePart file) {
        log.info("HTTP POST /api/v1/uploads received: filename={}", file.filename());

        if (file.filename() == null || file.filename().isBlank()) {
            return Mono.error(new IllegalArgumentException("File must have a valid filename"));
        }

        return fileUseCase.upload(file)
                .map(ResponseEntity.accepted()::body);
    }

    @GetMapping
    public Flux<FileResponse> getAll() {
        return fileUseCase.getAll().map(FileResponse::from);
    }
}
