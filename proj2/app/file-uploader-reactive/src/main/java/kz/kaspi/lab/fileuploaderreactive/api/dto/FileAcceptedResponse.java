package kz.kaspi.lab.fileuploaderreactive.api.dto;

import java.time.Instant;

public record FileAcceptedResponse(
        String requestId,
        String status,
        String message,
        Instant acceptedAt
) {
}
