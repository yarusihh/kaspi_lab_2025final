package kz.kaspi.lab.fileuploaderreactive.api.dto;

import java.time.Instant;

public record AcceptedUploadResponse(
        String requestId,
        String status,
        String message,
        Instant acceptedAt
) {
}
