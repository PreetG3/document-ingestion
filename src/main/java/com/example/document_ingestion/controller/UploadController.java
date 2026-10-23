package com.example.document_ingestion.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.document_ingestion.entity.Document;
import com.example.document_ingestion.repository.DocumentRepository;
import com.example.document_ingestion.service.ClamAvScanner;

@RestController
@RequestMapping("/api/documents")
public class UploadController {

    private final ClamAvScanner clamAvScanner;
    private final DocumentRepository documentRepository;

    private final Path uploadDirectory =
            Paths.get("uploads", "quarantine")
                    .toAbsolutePath()
                    .normalize();

    public UploadController(
            ClamAvScanner clamAvScanner,
            DocumentRepository documentRepository) {
        this.clamAvScanner = clamAvScanner;
        this.documentRepository = documentRepository;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", "Please select a file."));
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", "Maximum file size is 10 MB."));
        }

        String documentId = UUID.randomUUID().toString();
        Path destination = uploadDirectory.resolve(documentId);

        try {
            Files.createDirectories(uploadDirectory);

            try (var input = file.getInputStream()) {
                Files.copy(
                        input,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "documentId", documentId,
                            "status", "UPLOAD_FAILED",
                            "message", "Unable to save the uploaded file."));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unknown";
        }

        Document document = new Document(
                documentId,
                originalFilename,
                destination.getFileName().toString(),
                "QUARANTINED",
                LocalDateTime.now());

   
        try {
            documentRepository.save(document);
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "documentId", documentId,
                            "status", "QUARANTINED",
                            "message",
                            "Document metadata could not be saved. "
                                    + "File remains quarantined."));
        }

        try {
            boolean clean = clamAvScanner.isClean(destination);

            if (clean) {
                document.setStatus("CLEAN");
                documentRepository.save(document);

                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                                "documentId", documentId,
                                "status", "CLEAN",
                                "message",
                                "Antivirus scan passed. File remains "
                                        + "quarantined pending approval."));
            }

            document.setStatus("INFECTED");
            documentRepository.save(document);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "documentId", documentId,
                            "status", "INFECTED",
                            "message",
                            "ClamAV detected a threat. "
                                    + "File remains quarantined."));
        } catch (IOException e) {
            
            document.setStatus("QUARANTINED");

            try {
                documentRepository.save(document);
            } catch (RuntimeException saveError) {
               
            }

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "documentId", documentId,
                            "status", "QUARANTINED",
                            "message",
                            "Antivirus scanning failed. "
                                    + "File remains quarantined."));
        }
    }

   
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable String id) {
        return documentRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}