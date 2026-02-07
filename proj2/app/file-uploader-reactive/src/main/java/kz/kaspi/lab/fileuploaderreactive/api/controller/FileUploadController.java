package kz.kaspi.lab.fileuploaderreactive.api.controller;

import kz.kaspi.lab.fileuploaderreactive.api.dto.AcceptedUploadResponse;
import kz.kaspi.lab.fileuploaderreactive.application.port.in.FileUploadUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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
    public Mono<ResponseEntity<AcceptedUploadResponse>> upload(
            @RequestPart("file") FilePart file
    ) {
        if (file.filename() == null || file.filename().isBlank()) {
            return Mono.error(new IllegalArgumentException("File must have a valid filename"));
        }

        return fileUploadUseCase.upload(file)
                .map(ResponseEntity.accepted()::body);
    }
}
