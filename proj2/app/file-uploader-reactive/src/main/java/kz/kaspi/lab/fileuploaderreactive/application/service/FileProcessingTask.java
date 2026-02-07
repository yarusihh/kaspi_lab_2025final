package kz.kaspi.lab.fileuploaderreactive.application.service;

import java.nio.file.Path;

public record FileProcessingTask(
        String requestId,
        String fileName,
        String contentType,
        Path tempFile
) {
}
