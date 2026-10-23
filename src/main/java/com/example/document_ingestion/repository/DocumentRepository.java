
package com.example.document_ingestion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.document_ingestion.entity.Document;

public interface DocumentRepository
        extends JpaRepository<Document, String> {
}
