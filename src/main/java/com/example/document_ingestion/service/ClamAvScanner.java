
package com.example.document_ingestion.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ClamAvScanner {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 3310;
    private static final int TIMEOUT_MS = 30_000;

    public boolean isClean(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IOException("File does not exist.");
        }

        try (Socket socket = new Socket()) {
            socket.connect(
                new InetSocketAddress(HOST, PORT),
                5_000
            );
            socket.setSoTimeout(TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

           
            out.write("zINSTREAM\0".getBytes(
                java.nio.charset.StandardCharsets.US_ASCII
            ));

            byte[] buffer = new byte[8192];

            try (InputStream fileIn = Files.newInputStream(file)) {
                int count;

                while ((count = fileIn.read(buffer)) != -1) {
                    
                    out.write(new byte[] {
                        (byte) (count >>> 24),
                        (byte) (count >>> 16),
                        (byte) (count >>> 8),
                        (byte) count
                    });
                    out.write(buffer, 0, count);
                }
            }

            
            out.write(new byte[] {0, 0, 0, 0});
            out.flush();

            StringBuilder response = new StringBuilder();
            int b;

            while ((b = in.read()) != -1 && b != 0 && b != '\n') {
                response.append((char) b);
            }

            String result = response.toString().trim();

            if (result.endsWith(" OK")) {
                return true;
            }

            if (result.contains(" FOUND")) {
                return false;
            }

            
            throw new IOException(
                "ClamAV returned an unexpected response: " + result
            );
        }
    }
}
