package one.harshit.resumeTailor.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.harshit.resumeTailor.model.converter.PathConverter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ResumeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id;
    String fileName;
    LocalDateTime uploadDateTime;
    LocalDateTime lastModifiedDateTime;
    @Convert(converter = PathConverter.class)
    Path filePath;

    public ResumeDocument(String fileName, LocalDateTime uploadDateTime, Path filePath) {
        this.fileName = fileName;
        this.lastModifiedDateTime = LocalDateTime.now();
        this.filePath = filePath;
    }

}
