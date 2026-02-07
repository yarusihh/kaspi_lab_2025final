package kz.kaspi.lab.fileuploaderreactive.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dedup")
public record DedupProperties(
        Duration localTtl,
        Duration redisTtl
) {
}
