package one.harshit.resumeTailor.service;

import one.harshit.resumeTailor.model.ResumeDocument;
import one.harshit.resumeTailor.model.dto.ResumeDataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ResumeParserIntegrationTest {

    @TempDir
    Path tempDir;

    /**
     * Minimal valid raw PDF bytes adhering to the PDF 1.4 specification
     * containing a text stream with candidate details.
     */
    private Path createMinimalValidPdf(String candidateText) throws IOException {
        Path pdfPath = tempDir.resolve("sample_resume_" + System.nanoTime() + ".pdf");

        int textLength = candidateText.length() + 32;
        String template = """
                %PDF-1.4
                1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
                2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
                3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<</Font<</F1 4 0 R>>>>/Contents 5 0 R>>endobj
                4 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj
                5 0 obj<</Length {LENGTH}>>stream
                BT /F1 12 Tf 72 712 Td ({TEXT}) Tj ET
                endstream
                endobj
                xref
                0 6
                0000000000 65535 f\s
                0000000009 00000 n\s
                0000000052 00000 n\s
                0000000108 00000 n\s
                0000000216 00000 n\s
                0000000283 00000 n\s
                trailer<</Size 6/Root 1 0 R>>
                startxref
                377
                %%EOF
                """;

        String pdfContent = template
                .replace("{LENGTH}", String.valueOf(textLength))
                .replace("{TEXT}", candidateText);

        Files.write(pdfPath, pdfContent.getBytes(StandardCharsets.ISO_8859_1));
        return pdfPath;
    }

    @Test
    @DisplayName("Test 27: TikaDocumentReader parses real PDF and extracts text successfully")
    void tikaDocumentReader_RealPdfExtraction() throws IOException {
        // Given a valid PDF containing candidate details
        Path pdfPath = createMinimalValidPdf("Jane Doe - Senior Backend Engineer");

        // When reading through TikaDocumentReader
        TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(pdfPath));
        List<Document> documents = reader.get();

        // Then
        assertThat(documents).isNotEmpty();
        String extractedText = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        assertThat(extractedText).contains("Jane Doe");
        assertThat(extractedText).contains("Senior Backend Engineer");
    }

    @Test
    @DisplayName("Test 28: ResumeParserService end-to-end extraction from real PDF file into ChatClient")
    @SuppressWarnings("unchecked")
    void resumeParserService_TikaExtractionIntegration() throws IOException {
        // Given
        Path pdfPath = createMinimalValidPdf("Alex Mercer - Cloud Solutions Architect");

        ResumeDocument resumeDocument = new ResumeDocument();
        resumeDocument.setFileName("alex_mercer.pdf");
        resumeDocument.setFilePath(pdfPath);

        ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class, Answers.RETURNS_DEEP_STUBS);
        ChatClient mockChatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(mockBuilder.build()).thenReturn(mockChatClient);

        ResumeDataDto expectedDto = new ResumeDataDto(
                "Cloud Solutions Architect with 8+ years experience",
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(mockChatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .call()
                .entity(ResumeDataDto.class))
                .thenReturn(expectedDto);

        // When
        ResumeParserService service = new ResumeParserService(mockBuilder);
        ResumeDataDto result = service.parseFile(resumeDocument);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.summary()).isEqualTo("Cloud Solutions Architect with 8+ years experience");
        verify(mockChatClient, atLeastOnce()).prompt();
    }
}