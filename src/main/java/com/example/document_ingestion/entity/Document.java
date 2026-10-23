
package com.example.document_ingestion.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    private String id;

    private String originalFilename;

    private String storedFilename;

    private String status;

    private LocalDateTime uploadedAt;

    public Document() {
    }

    public Document(String id, String originalFilename,
                    String storedFilename, String status,
                    LocalDateTime uploadedAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.status = status;
        this.uploadedAt = uploadedAt;
    }

    public String getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
