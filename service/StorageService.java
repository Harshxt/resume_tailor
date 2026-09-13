package one.harshit.resumeTailor.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

import one.harshit.resumeTailor.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import one.harshit.resumeTailor.model.ResumeDocument;

@Service
public class StorageService {
    private final DocumentRepository documentRepository;
    private final static Logger log = LoggerFactory.getLogger(StorageService.class);
    private final Path rootLocation = Paths.get("./files/upload/");

    public StorageService(DocumentRepository documentRepository) {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            log.error("Error creating a directory at {}: {}", rootLocation, e.getMessage());
        }
        this.documentRepository = documentRepository;
    }

    public ResumeDocument store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");

        }

        Path filePath = writeFile(file);

        ResumeDocument document= new ResumeDocument();
        document.setFileName(filePath.getFileName().toString());
        document.setUploadDateTime(LocalDateTime.now());
        document.setLastModifiedDateTime(LocalDateTime.now());
        document.setFilePath(filePath);

        return documentRepository.save(document);

        
    }

    private Path writeFile(MultipartFile file) {
        try {
            String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();

            String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;

            Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize().toAbsolutePath();

            if (!destinationFile.startsWith(this.rootLocation.normalize().toAbsolutePath()))
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
