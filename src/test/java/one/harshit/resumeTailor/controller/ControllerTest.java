package one.harshit.resumeTailor.controller;

import one.harshit.resumeTailor.exception.advice.GlobalExceptionHandler;
import one.harshit.resumeTailor.model.ResumeDocument;
import one.harshit.resumeTailor.model.dto.ResumeDataDto;
import one.harshit.resumeTailor.service.LlmService;
import one.harshit.resumeTailor.service.ResumeParserService;
import one.harshit.resumeTailor.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ControllerTest {

    @Mock
    private ResumeParserService resumeParserService;

    @Mock
    private StorageService storageService;

    @Mock
    private LlmService llmService;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        Controller controller = new Controller(resumeParserService, objectMapper, storageService, llmService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ResumeDataDto createSampleResumeData() {
        return new ResumeDataDto(
                "Software Engineer",
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Test 20: GET /api/resume/health returns HTTP 200 with 'All good'")
    void healthCheck_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/resume/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("All good"));
    }

    @Test
    @DisplayName("Test 21: POST /api/resume/upload with empty file returns 400 Bad Request")
    void uploadResume_EmptyFile_ReturnsBadRequest() throws Exception {
        // Given an empty file and valid data JSON part
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                new byte[0]
        );
        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"jobDescription\":\"Looking for Java Dev\",\"targetRole\":\"Java Developer\"}".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        mockMvc.perform(multipart("/api/resume/upload")
                        .file(emptyFile)
                        .file(dataPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Uploaded file is empty"));

        verify(storageService, never()).store(any());
    }

    @Test
    @DisplayName("Test 22: POST /api/resume/upload when isResume returns false returns 400 Bad Request")
    void uploadResume_NonResumeFile_ReturnsBadRequest() throws Exception {
        // Given a file that is not classified as a resume by LLM
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "recipe.pdf",
                "application/pdf",
                "Pancake recipe".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"jobDescription\":\"Some JD\",\"targetRole\":\"Developer\"}".getBytes(StandardCharsets.UTF_8)
        );

        ResumeDocument document = new ResumeDocument();
        document.setFileName("recipe.pdf");
        ResumeDataDto nonResumeData = createSampleResumeData();

        when(storageService.store(any())).thenReturn(document);
        when(resumeParserService.parseFile(document)).thenReturn(nonResumeData);
        when(llmService.isResume(nonResumeData)).thenReturn(false);

        // When & Then
        mockMvc.perform(multipart("/api/resume/upload")
                        .file(file)
                        .file(dataPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("The document provided is not a resume"));

        verify(llmService, never()).suggestChanges(any(), any(), any());
    }

    @Test
    @DisplayName("Test 23: POST /api/resume/upload with valid resume returns 200 OK with suggestions")
    void uploadResume_ValidResume_ReturnsOkWithSuggestions() throws Exception {
        // Given valid resume file and data part
        String filename = "john_doe_resume.pdf";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "application/pdf",
                "Valid resume binary".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"jobDescription\":\"Seeking Cloud Engineer\",\"targetRole\":\"Cloud Engineer\"}".getBytes(StandardCharsets.UTF_8)
        );

        ResumeDocument document = new ResumeDocument();
        document.setFileName(filename);
        ResumeDataDto resumeData = createSampleResumeData();

        when(storageService.store(any())).thenReturn(document);
        when(resumeParserService.parseFile(document)).thenReturn(resumeData);
        when(llmService.isResume(resumeData)).thenReturn(true);
        when(llmService.debloatJobDescription("Seeking Cloud Engineer")).thenReturn("Cleaned JD");
        when(llmService.suggestChanges(resumeData, "Cleaned JD", "Cloud Engineer"))
                .thenReturn("Emphasize AWS Lambda and Kubernetes experience.");

        // When & Then
        mockMvc.perform(multipart("/api/resume/upload")
                        .file(file)
                        .file(dataPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Received file: " + filename))
                .andExpect(jsonPath("$.data.suggestions").value("Emphasize AWS Lambda and Kubernetes experience."));

        verify(storageService, times(1)).store(any());
        verify(resumeParserService, times(1)).parseFile(document);
        verify(llmService, times(1)).isResume(resumeData);
        verify(llmService, times(1)).debloatJobDescription("Seeking Cloud Engineer");
        verify(llmService, times(1)).suggestChanges(resumeData, "Cleaned JD", "Cloud Engineer");
    }

    @Test
    @DisplayName("Test 24: POST /api/resume/upload with minimal/blank data fields executes cleanly")
    void uploadResume_MinimalDataFields_HandlesCleanly() throws Exception {
        // Given valid file with empty strings in JD and role
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "minimal.pdf",
                "application/pdf",
                "content".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"jobDescription\":\"\",\"targetRole\":\"\"}".getBytes(StandardCharsets.UTF_8)
        );

        ResumeDocument document = new ResumeDocument();
        document.setFileName("minimal.pdf");
        ResumeDataDto resumeData = createSampleResumeData();

        when(storageService.store(any())).thenReturn(document);
        when(resumeParserService.parseFile(document)).thenReturn(resumeData);
        when(llmService.isResume(resumeData)).thenReturn(true);
        when(llmService.debloatJobDescription("")).thenReturn("");
        when(llmService.suggestChanges(resumeData, "", "")).thenReturn("General resume recommendations.");

        // When & Then
        mockMvc.perform(multipart("/api/resume/upload")
                        .file(file)
                        .file(dataPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.suggestions").value("General resume recommendations."));
    }
}