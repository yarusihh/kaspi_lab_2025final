package kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.repository;

import java.util.UUID;
import kz.kaspi.lab.fileuploaderreactive.infrastructure.persistence.entity.FileRecordEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface FileRecordR2dbcRepository extends ReactiveCrudRepository<FileRecordEntity, UUID> {
}
