package kz.kaspi.lab.fileuploaderreactive.api.dto;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
}
