package kz.kaspi.lab.fileuploaderreactive.infrastructure.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3ClientConfig {

    @Bean(destroyMethod = "close")
    public S3AsyncClient s3AsyncClient(StorageProperties properties) {
        String endpoint = normalizeEndpoint(properties.endpoint(), properties.sslEnabled());

        return S3AsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyle())
                        .build())
                .build();
    }

    private String normalizeEndpoint(String endpoint, boolean sslEnabled) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("app.storage.endpoint must not be blank");
        }

        if (sslEnabled && endpoint.startsWith("http://")) {
            return "https://" + endpoint.substring("http://".length());
        }
        if (!sslEnabled && endpoint.startsWith("https://")) {
            return "http://" + endpoint.substring("https://".length());
        }
        return endpoint;
    }
}
