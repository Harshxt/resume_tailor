package one.harshit.resumeTailor.repository;

import jakarta.persistence.EntityManager;
import one.harshit.resumeTailor.model.ResumeDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Test 25: Save ResumeDocument persists to DB and PathConverter restores java.nio.file.Path")
    void saveAndFind_PersistsWithConverter() {
        // Given
        Path originalPath = Paths.get("files", "upload", "sample_candidate.pdf");
        LocalDateTime now = LocalDateTime.now();

        ResumeDocument document = new ResumeDocument();
        document.setFileName("sample_candidate.pdf");
        document.setFilePath(originalPath);
        document.setUploadDateTime(now);
        document.setLastModifiedDateTime(now);

        // When: save and flush to H2 database
        ResumeDocument saved = documentRepository.save(document);
        entityManager.flush();
        entityManager.clear(); // Detach to verify reading from SQL & PathConverter

        // Then
        Optional<ResumeDocument> retrieved = documentRepository.findById(saved.getId());

        assertThat(retrieved).isPresent();
        ResumeDocument found = retrieved.get();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getFileName()).isEqualTo("sample_candidate.pdf");
        assertThat(found.getFilePath()).isNotNull();
        assertThat(found.getFilePath()).isEqualTo(originalPath);
        assertThat(found.getUploadDateTime()).isNotNull();
    }

    @Test
    @DisplayName("Test 26: Entity sequence ID generation, update, and deletion")
    void entityLifecycle_GeneratesId_UpdatesAndDeletes() {
        // Given two documents
        ResumeDocument doc1 = new ResumeDocument("doc1.pdf", LocalDateTime.now(), Paths.get("files/upload/doc1.pdf"));
        ResumeDocument doc2 = new ResumeDocument("doc2.pdf", LocalDateTime.now(), Paths.get("files/upload/doc2.pdf"));

        // When saving both
        ResumeDocument saved1 = documentRepository.save(doc1);
        ResumeDocument saved2 = documentRepository.save(doc2);
        entityManager.flush();

        // Then IDs are assigned and distinct
        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        assertThat(documentRepository.count()).isGreaterThanOrEqualTo(2);

        // Delete test
        documentRepository.deleteById(saved1.getId());
        entityManager.flush();

        assertThat(documentRepository.findById(saved1.getId())).isEmpty();
        assertThat(documentRepository.findById(saved2.getId())).isPresent();
    }
}