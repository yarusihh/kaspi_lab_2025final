package kz.kaspi.lab.fileuploaderreactive;

import kz.kaspi.lab.fileuploaderreactive.infrastructure.config.DedupProperties;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.config.StorageProperties;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.config.UploadProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({UploadProperties.class, StorageProperties.class, DedupProperties.class})
public class FileUploaderReactiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileUploaderReactiveApplication.class, args);
    }
}
