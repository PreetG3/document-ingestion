
package com.example.document_ingestion.service;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class WhatsAppService {

    public Map<String, String> receiveDocument(String filename) {
        return Map.of(
                "source", "WHATSAPP_SIMULATED",
                "filename", filename,
                "status", "RECEIVED",
                "message",
                "Simulated WhatsApp notification received. "
                        + "Actual file download is not implemented yet.");
    }
}