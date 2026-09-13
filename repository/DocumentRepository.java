package one.harshit.resumeTailor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import one.harshit.resumeTailor.model.ResumeDocument;

public interface DocumentRepository extends JpaRepository<ResumeDocument, Long> {
    
    
}
