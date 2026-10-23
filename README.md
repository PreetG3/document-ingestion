# Document Ingestion System

## Overview
A Java Spring Boot application that receives documents, scans them for malware using ClamAV, and stores document details in an H2 database.

## Technologies Used
- Java 21
- Spring Boot
- Spring Data JPA
- H2 Database
- ClamAV
- Gmail IMAP
- Maven

## Features
- Upload documents through a REST API.
- Scan uploaded documents using ClamAV.
- Store document metadata in an H2 database.
- Check Gmail for unread email attachments.
- Simulate document reception through a WhatsApp upload endpoint.
- Retrieve document details using an ID.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/documents/upload` | Upload and scan a document |
| GET | `/api/documents/{id}` | Retrieve document details |
| POST | `/api/email/check` | Check Gmail for attachments |
| POST | `/api/whatsapp/receive` | Simulate receiving a document through WhatsApp |


## Testing
Use Thunder Client or another REST client to test the API endpoints.

