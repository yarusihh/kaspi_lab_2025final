package kz.kaspi.lab.fileuploaderreactive.application.service;

import java.nio.file.Path;

public record UploadTask(
        String requestId,
        String fileName,
        String contentType,
        Path tempFile
) {
}
