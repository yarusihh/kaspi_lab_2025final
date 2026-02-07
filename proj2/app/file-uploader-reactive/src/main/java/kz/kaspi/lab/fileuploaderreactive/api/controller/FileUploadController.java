package kz.kaspi.lab.fileuploaderreactive.api.controller;

import kz.kaspi.lab.fileuploaderreactive.api.dto.UploadResponse;
import kz.kaspi.lab.fileuploaderreactive.application.port.in.FileUploadUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/v1/uploads")
public class FileUploadController {

    private final FileUploadUseCase fileUploadUseCase;

    public FileUploadController(FileUploadUseCase fileUploadUseCase) {
        this.fileUploadUseCase = fileUploadUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<UploadResponse>> upload(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestPart("file") FilePart file
    ) {
        return fileUploadUseCase.upload(clientId, idempotencyKey, file)
                .map(UploadResponse::from)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{uploadId}")
    public Mono<ResponseEntity<UploadResponse>> getStatus(@PathVariable String uploadId) {
        return fileUploadUseCase.getById(uploadId)
                .map(UploadResponse::from)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
