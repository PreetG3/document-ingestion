
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.document_ingestion.entity.Document;
import com.example.document_ingestion.repository.DocumentRepository;
import com.example.document_ingestion.service.ClamAvScanner;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final ClamAvScanner clamAvScanner;
    private final DocumentRepository documentRepository;

    private final Path uploadDirectory =
            Paths.get("uploads", "quarantine")
                    .toAbsolutePath()
                    .normalize();

    public WhatsAppController(
            ClamAvScanner clamAvScanner,
            DocumentRepository documentRepository) {
        this.clamAvScanner = clamAvScanner;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/receive")
    public ResponseEntity<Map<String, String>> receiveDocument(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Please select a file."));
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Maximum file size is 10 MB."));
        }

        String documentId = UUID.randomUUID().toString();
        Path destination = uploadDirectory.resolve(documentId);

        try {
            Files.createDirectories(uploadDirectory);

            try (var input = file.getInputStream()) {
                Files.copy(input, destination,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Unable to save file."));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "unknown";
        }

        Document document = new Document(
                documentId,
                filename,
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
                            "message", "Could not save document metadata."));
        }

        try {
            boolean clean = clamAvScanner.isClean(destination);

            document.setStatus(clean ? "CLEAN" : "INFECTED");
            documentRepository.save(document);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "documentId", documentId,
                            "source", "WHATSAPP_SIMULATED",
                            "status", document.getStatus(),
                            "message", clean
                                    ? "File stored and antivirus scan passed."
                                    : "Threat detected. File remains quarantined."));
        } catch (IOException e) {
            document.setStatus("QUARANTINED");

            try {
                documentRepository.save(document);
            } catch (RuntimeException ignored) {
                
            }

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "documentId", documentId,
                            "status", "QUARANTINED",
                            "message", "Antivirus scan failed."));
        }
    }
}