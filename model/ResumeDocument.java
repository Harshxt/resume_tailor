package one.harshit.resumeTailor.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;


@Data 
@Entity 
public class ResumeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    String id;
    String fileName;
    LocalDateTime uploadDateTime;
    LocalDateTime lastModifiedDateTime;
    Path filePath;


    public ResumeDocument(String fileName, LocalDateTime uploadDateTime,  Path filePath) {
        this.fileName = fileName;
        this.lastModifiedDateTime = LocalDateTime.now();
        this.filePath = filePath;
    }
    

    


}
