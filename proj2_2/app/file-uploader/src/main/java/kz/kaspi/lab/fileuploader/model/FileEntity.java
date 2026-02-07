package kz.kaspi.lab.fileuploader.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("files")
public class FileEntity {

    @Id
    private Long id;

    private UUID uuid;
    private LocalDateTime uploadDate;
    private String s3Key;
    private String name;
    private Long size;
    private String hash;

    public FileEntity() {}

    public FileEntity(
            Long id,
            UUID uuid,
            LocalDateTime uploadDate,
            String s3Key,
            String name,
            Long size,
            String hash
    ) {
        this.id = id;
        this.uuid = uuid;
        this.uploadDate = uploadDate;
        this.s3Key = s3Key;
        this.name = name;
        this.size = size;
        this.hash = hash;
    }

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public LocalDateTime getUploadDate() { return uploadDate; }
    public String getS3Key() { return s3Key; }
    public String getName() { return name; }
    public Long getSize() { return size; }
    public String getHash() { return hash; }

    public void setId(Long id) { this.id = id; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public void setName(String name) { this.name = name; }
    public void setSize(Long size) { this.size = size; }
    public void setHash(String hash) { this.hash = hash; }
}
