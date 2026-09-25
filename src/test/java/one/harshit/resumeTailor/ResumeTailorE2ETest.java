package one.harshit.resumeTailor;

import one.harshit.resumeTailor.model.ResumeDocument;
import one.harshit.resumeTailor.repository.DocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ResumeTailorE2ETest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DocumentRepository documentRepository;

    private MockMvc mockMvc;
    private final List<Path> filesToCleanUp = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    void tearDown() {
        // Clean up files created in ./files/upload/ during the tests
        for (Path file : filesToCleanUp) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Generates a minimal valid PDF byte sequence for end-to-end testing.
     */
    private byte[] createMinimalValidPdf(String text) {
        int textLength = text.length() + 32;
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
                .replace("{TEXT}", text);

        return pdfContent.getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    @DisplayName("Test 31: Full E2E Resume Tailoring Flow (Upload -> Storage -> DB -> Tika -> LLM -> Response)")
    void resumeTailor_FullUploadPipeline_E2E() throws Exception {
        // Given: A valid resume PDF with candidate details
        byte[] pdfBytes = createMinimalValidPdf(
                "Jane Doe | Senior Backend Engineer | Java, Spring Boot, Microservices, PostgreSQL"
        );
        String originalFilename = "jane_doe_e2e_resume.pdf";

        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                originalFilename,
                "application/pdf",
                pdfBytes
        );

        String jsonPayload = """
                {
                    "jobDescription": "We are looking for a Senior Java Developer with Spring Boot and PostgreSQL expertise.",
                    "targetRole": "Senior Java Developer"
                }
                """;

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                jsonPayload.getBytes(StandardCharsets.UTF_8)
        );

        long initialDbCount = documentRepository.count();

        // When: Executing full HTTP POST multipart request through the live Spring pipeline
        mockMvc.perform(multipart("/api/resume/upload")
                        .file(filePart)
                        .file(dataPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Received file: " + originalFilename))
                .andExpect(jsonPath("$.data.suggestions").isString());

        // Then: Verify database record was persisted in H2
        assertThat(documentRepository.count()).isEqualTo(initialDbCount + 1);

        List<ResumeDocument> allDocuments = documentRepository.findAll();
        ResumeDocument savedRecord = allDocuments.stream()
                .filter(d -> d.getFileName() != null && d.getFileName().endsWith(originalFilename))
                .findFirst()
                .orElse(null);

        assertThat(savedRecord).isNotNull();
        assertThat(savedRecord.getFilePath()).isNotNull();

        // Verify physical file was written to disk
        filesToCleanUp.add(savedRecord.getFilePath());
        assertThat(Files.exists(savedRecord.getFilePath())).isTrue();
    }

    @Test
    @DisplayName("Test 32: E2E Health Check and Empty File Validation Flow")
    void resumeTailor_HealthCheckAndEmptyFileValidation_E2E() throws Exception {
        // Sub-test A: Live health check endpoint
        mockMvc.perform(get("/api/resume/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("All good"));

        // Sub-test B: Sending empty file through live stack returns 400 Bad Request
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );
        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"jobDescription\":\"JD\",\"targetRole\":\"Role\"}".getBytes(StandardCharsets.UTF_8)
        );

        long countBefore = documentRepository.count();

        mockMvc.perform(multipart("/api/resume/upload")
                        .file(emptyFile)
                        .file(dataPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Uploaded file is empty"));

        // Verify no orphan records were created in the database
        assertThat(documentRepository.count()).isEqualTo(countBefore);
    }
}