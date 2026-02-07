package kz.kaspi.lab.fileuploaderreactive.api.dto;

import java.time.Instant;
import kz.kaspi.lab.fileuploaderreactive.domain.model.FileRecord;

public record FileResponse(
        String id,
        String checksum,
        String fileName,
        String contentType,
        String status,
        Instant createdAt
) {
    public static FileResponse from(FileRecord record) {
        return new FileResponse(
                record.id().toString(),
                record.checksum(),
                record.fileName(),
                record.contentType(),
                record.status().name(),
                record.createdAt()
        );
    }
}
