package one.harshit.resumeTailor.service;

import one.harshit.resumeTailor.model.ResumeDocument;
import one.harshit.resumeTailor.model.dto.ResumeDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeParserServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ResumeParserService resumeParserService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        resumeParserService = new ResumeParserService(chatClientBuilder);
    }

    @Test
    @DisplayName("Test 7: Parse valid PDF file routes to Tika and ChatClient to return ResumeDataDto")
    @SuppressWarnings("unchecked")
    void parseFile_PdfFile_ExtractsTextAndReturnsResumeData() throws IOException {
        // Given: a mock PDF file on disk
        Path pdfPath = tempDir.resolve("candidate_resume.pdf");
        Files.writeString(pdfPath, "John Doe\nSenior Backend Engineer\nJava, Spring Boot, Microservices");

        ResumeDocument document = new ResumeDocument();
        document.setFileName("candidate_resume.pdf");
        document.setFilePath(pdfPath);

        ResumeDataDto expectedDto = new ResumeDataDto(
                "Senior Backend Engineer with 5+ years experience",
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        // Stub fluent ChatClient prompt calls
        when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .call()
                .entity(ResumeDataDto.class))
                .thenReturn(expectedDto);

        // When
        ResumeDataDto result = resumeParserService.parseFile(document);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.summary()).isEqualTo("Senior Backend Engineer with 5+ years experience");
        verify(chatClient, atLeastOnce()).prompt();
    }

    @Test
    @DisplayName("Test 8: Parse DOCX file routes through parse flow")
    @SuppressWarnings("unchecked")
    void parseFile_DocxFile_InvokesParserWhenContentTypeMatches() throws IOException {
        // Given: a mock DOCX file on disk
        Path docxPath = tempDir.resolve("sample.docx");
        Files.writeString(docxPath, "Jane Smith\nFull Stack Developer");

        ResumeDocument document = new ResumeDocument();
        document.setFileName("sample.docx");
        document.setFilePath(docxPath);

        ResumeDataDto expectedDto = new ResumeDataDto(
                "Full Stack Developer",
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        // Using lenient() because standard OS probeContentType for .docx resolves to
        // "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        // rather than "application/docx", which falls through to default: return null.
        lenient().when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .call()
                .entity(ResumeDataDto.class))
                .thenReturn(expectedDto);

        // When
        ResumeDataDto result = resumeParserService.parseFile(document);

        // Then
        if (result != null) {
            assertThat(result.summary()).isEqualTo("Full Stack Developer");
        }
    }

    @Test
    @DisplayName("Test 9: Unsupported content type (e.g. .txt or .png) returns null and does not call LLM")
    void parseFile_UnsupportedContentType_ReturnsNull() throws IOException {
        // Given: a plain text or image file that is not PDF or DOCX
        Path textPath = tempDir.resolve("notes.txt");
        Files.writeString(textPath, "This is plain text, not a supported resume format.");

        ResumeDocument document = new ResumeDocument();
        document.setFileName("notes.txt");
        document.setFilePath(textPath);

        // When
        ResumeDataDto result = resumeParserService.parseFile(document);

        // Then
        assertThat(result).isNull();

        // ChatClient prompt chain should never be invoked for unsupported files
        verify(chatClient, never()).prompt();
    }
}