package kz.kaspi.lab.fileuploaderreactive.api.dto;

import java.time.Instant;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileUploadRecord;

public record UploadResponse(
        String uploadId,
        String fileName,
        String storageObjectKey,
        String status,
        Instant createdAt
) {
    public static UploadResponse from(FileUploadRecord record) {
        return new UploadResponse(
                record.id().toString(),
                record.fileName(),
                record.storageObjectKey(),
                record.status().name(),
                record.createdAt()
        );
    }
}
