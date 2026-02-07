package kz.kaspi.lab.fileuploaderreactive.domain.model;

import java.time.Instant;
import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.domain.value.FileStatus;

public record FileRecord(
        UUID id,
        String checksum,
        String fileName,
        String contentType,
        long sizeBytes,
        String storageObjectKey,
        FileStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
