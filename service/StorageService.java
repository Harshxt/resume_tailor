package one.harshit.resumeTailor.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.AbstractLogger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
    private final static Logger log = LoggerFactory.getLogger(StorageService.class);
    private final Path rootLocation = Paths.get("./files/upload/");

    public StorageService() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            log.error("Error creating a directory at {}: {}", rootLocation, e.getMessage());
        }
    }

    public Path store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");

        }
        return writeFile(file);
    }

    private Path writeFile(MultipartFile file) {
        try {
            String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();

            String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;

            Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize().toAbsolutePath();

            if (!destinationFile.startsWith(this.rootLocation.toAbsolutePath()))
                throw new SecurityException("Cannot store file outside target directory.");

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return destinationFile;
        } catch (IOException e) {
            log.error("Failed to store file {}", e.getMessage());
            throw new RuntimeException("Failed to store file", e);
        }

    }
}
