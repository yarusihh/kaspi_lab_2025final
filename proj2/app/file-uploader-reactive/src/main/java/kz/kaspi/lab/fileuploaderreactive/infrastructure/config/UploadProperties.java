package kz.kaspi.lab.fileuploaderreactive.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
        String maxFileSize,
        String maxRequestSize,
        Duration timeout
) {
}
