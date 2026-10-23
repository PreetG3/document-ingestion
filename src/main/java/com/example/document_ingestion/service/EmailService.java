
package com.example.document_ingestion.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FlagTerm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.document_ingestion.entity.Document;
import com.example.document_ingestion.repository.DocumentRepository;

@Service
public class EmailService {

    @Value("${gmail.username}")
    private String username;

    @Value("${gmail.app-password}")
    private String appPassword;

    private final ClamAvScanner clamAvScanner;
    private final DocumentRepository documentRepository;

    private final Path uploadDirectory =
            Paths.get("uploads", "quarantine")
                    .toAbsolutePath()
                    .normalize();

    public EmailService(
            ClamAvScanner clamAvScanner,
            DocumentRepository documentRepository) {
        this.clamAvScanner = clamAvScanner;
        this.documentRepository = documentRepository;
    }

    public List<String> checkEmails() throws Exception {
        List<String> results = new ArrayList<>();

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", "imap.gmail.com");
        properties.put("mail.imaps.port", "993");
        properties.put("mail.imaps.ssl.enable", "true");
        properties.put("mail.imaps.connectiontimeout", "10000");
        properties.put("mail.imaps.timeout", "10000");

        Session session = Session.getInstance(properties);

        try (Store store = session.getStore("imaps")) {
            store.connect("imap.gmail.com", username, appPassword);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            try {
                Message[] messages = inbox.search(
                        new FlagTerm(new Flags(Flags.Flag.SEEN), false));

                for (Message message : messages) {
                    boolean hasAttachment = false;

                    if (message.isMimeType("multipart/*")) {
                        Multipart multipart =
                                (Multipart) message.getContent();

                        for (int i = 0; i < multipart.getCount(); i++) {
                            BodyPart part = multipart.getBodyPart(i);

                            String filename = part.getFileName();

                            boolean isAttachment =
                                    Part.ATTACHMENT.equalsIgnoreCase(
                                            part.getDisposition())
                                    || (filename != null
                                            && !filename.isBlank());

                            if (!isAttachment) {
                                continue;
                            }

                            hasAttachment = true;

                            if (filename == null || filename.isBlank()) {
                                filename = "email-attachment";
                            }

                            String documentId =
                                    saveAndScan(part, filename);

                            results.add(
                                    filename + " -> " + documentId);
                        }
                    }

                    
                    if (hasAttachment) {
                        message.setFlag(Flags.Flag.SEEN, true);
                    }
                }
            } finally {
                inbox.close(false);
            }
        }

        return results;
    }

    private String saveAndScan(Part part, String filename)
            throws Exception {

        String documentId = UUID.randomUUID().toString();
        Path destination = uploadDirectory.resolve(documentId);

        Files.createDirectories(uploadDirectory);

        try (InputStream input = part.getInputStream()) {
            Files.copy(
                    input,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING);
        }

        Document document = new Document(
                documentId,
                filename,
                destination.getFileName().toString(),
                "QUARANTINED",
                LocalDateTime.now());

        documentRepository.save(document);

        try {
            boolean clean = clamAvScanner.isClean(destination);

            document.setStatus(clean ? "CLEAN" : "INFECTED");
            documentRepository.save(document);
        } catch (Exception e) {
            document.setStatus("QUARANTINED");
            documentRepository.save(document);
            throw e;
        }

        return documentId;
    }
}
