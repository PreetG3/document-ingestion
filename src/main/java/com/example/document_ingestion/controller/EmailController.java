package com.example.document_ingestion.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.document_ingestion.service.EmailService;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkEmails() {
        try {
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Email check completed.",
                            "attachments", emailService.checkEmails()));
        } catch (Exception e) {
            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "message",
                            "Could not check Gmail. Verify IMAP settings "
                                    + "and credentials."));
        }
    }
}
