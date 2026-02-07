package kz.kaspi.lab.fileuploaderreactive.domain.model;

import java.time.Instant;
import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.domain.value.UploadStatus;

public record FileUploadRecord(
        UUID id,
        String checksum,
        String fileName,
        String contentType,
        long sizeBytes,
        String storageObjectKey,
        UploadStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
