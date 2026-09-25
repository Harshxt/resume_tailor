package one.harshit.resumeTailor.service;

import one.harshit.resumeTailor.model.ResumeDocument;
import one.harshit.resumeTailor.repository.DocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {
    @Mock
    private DocumentRepository documentRepository;

    private StorageService storageService;
    private final List<Path> createdFiles = new ArrayList<>();

    @BeforeEach
    void setUp() {
        storageService = new StorageService(documentRepository);
    }

    @AfterEach
    void tearDown() {
        // Clean up any test files created on disk
        for (Path file : createdFiles) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    @DisplayName("Test 3: Store valid MultipartFile saves to disk and persists ResumeDocument")
    void store_ValidFile_SavesFileAndPersistsDocument() {
        // Given
        String originalFilename = "sample_resume.pdf";
        byte[] content = "%PDF-1.4 dummy pdf content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                originalFilename,
                "application/pdf",
                content);

        when(documentRepository.save(any(ResumeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResumeDocument savedDocument = storageService.store(file);

        assertThat(savedDocument).isNotNull();
        assertThat(savedDocument.getFilePath()).isNotNull();
        createdFiles.add(savedDocument.getFilePath());

        assertThat(Files.exists(savedDocument.getFilePath())).isTrue();
        assertThat(savedDocument.getFileName()).endsWith("_" + originalFilename);
        assertThat(savedDocument.getUploadDateTime()).isNotNull();
        assertThat(savedDocument.getLastModifiedDateTime()).isNotNull();
        verify(documentRepository, times(1)).save(any(ResumeDocument.class));

    }

    @Test
    @DisplayName("Test 4: Store empty file throws IllegalArgumentException")
    void store_EmptyFile_ThrowsIllegalArgumentException() {
        // Given an empty file
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]);
        // When & Then
        assertThatThrownBy(() -> storageService.store(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to store empty file.");
        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 5: Filename with path traversal does not escape upload directory")
    void store_PathTraversalFilename_StaysWithinUploadDirectory() {
        // Given a malicious filename with path traversal characters
        String maliciousFilename = "../../etc/passwd.pdf";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                maliciousFilename,
                "application/pdf",
                "malicious test content".getBytes());

        when(documentRepository.save(any(ResumeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ResumeDocument savedDocument = storageService.store(file);

        // Then
        assertThat(savedDocument).isNotNull();
        Path savedPath = savedDocument.getFilePath();
        createdFiles.add(savedPath);

        Path rootDir = Paths.get("./files/upload/").normalize().toAbsolutePath();
        assertThat(savedPath.normalize().toAbsolutePath().startsWith(rootDir)).isTrue();
        assertThat(savedPath.getFileName().toString()).doesNotContain("..");
    }

    @Test
    @DisplayName("Test 6: IOException during file write is wrapped in RuntimeException")
    void store_IoException_ThrowsRuntimeException() throws IOException {
        // Given a file whose InputStream fails
        MultipartFile failingFile = mock(MultipartFile.class);
        when(failingFile.isEmpty()).thenReturn(false);
        when(failingFile.getOriginalFilename()).thenReturn("test.pdf");
        when(failingFile.getInputStream()).thenThrow(new IOException("Simulated disk error"));

        assertThatThrownBy(() -> storageService.store(failingFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to store file")
                .hasCauseInstanceOf(IOException.class);
        verify(documentRepository, never()).save(any());
    }

}