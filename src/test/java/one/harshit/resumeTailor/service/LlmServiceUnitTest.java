package one.harshit.resumeTailor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import one.harshit.resumeTailor.model.dto.ResumeDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmServiceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ObjectMapper objectMapper;
    private LlmService llmService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(chatClientBuilder.build()).thenReturn(chatClient);
        llmService = new LlmService(chatClientBuilder, objectMapper);
    }

    private ResumeDataDto createSampleResumeData() {
        return new ResumeDataDto(
                "Senior Software Engineer",
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Test 10: isResume(ResumeDataDto) returns true when LLM confirms it is a resume")
    @SuppressWarnings("unchecked")
    void isResume_ResumeDataDto_ValidResume_ReturnsTrue() {
        // Given
        ResumeDataDto sampleResume = createSampleResumeData();
        when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .call()
                .content())
                .thenReturn("true");

        // When
        boolean result = llmService.isResume(sampleResume);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Test 11: isResume(ResumeDataDto) returns false for null DTO, negative classification, or null response")
    @SuppressWarnings("unchecked")
    void isResume_ResumeDataDto_NullOrNegativeClassification_ReturnsFalse() {
        // Case A: null DTO returns false immediately without calling LLM
        boolean nullResult = llmService.isResume((ResumeDataDto) null);
        assertThat(nullResult).isFalse();

        // Case B: LLM responds "false"
        when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .call()
                .content())
                .thenReturn("false");

        boolean falseResult = llmService.isResume(createSampleResumeData());
        assertThat(falseResult).isFalse();

        // Case C: LLM response is null
        when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .call()
                .content())
                .thenReturn(null);

        boolean nullLlmResult = llmService.isResume(createSampleResumeData());
        assertThat(nullLlmResult).isFalse();
    }

    @Test
    @DisplayName("Test 12: isResume(ResumeDataDto) catches serialization and LLM exceptions and returns false safely")
    @SuppressWarnings("unchecked")
    void isResume_ResumeDataDto_HandlesExceptionsGracefully() throws JsonProcessingException {
        // Case A: LLM call throws runtime exception
        when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .call()
                .content())
                .thenThrow(new RuntimeException("AI provider timeout"));

        boolean exceptionResult = llmService.isResume(createSampleResumeData());
        assertThat(exceptionResult).isFalse();

        // Case B: JSON serialization failure (using doAnswer to simulate JsonProcessingException)
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        ObjectWriter failingWriter = mock(ObjectWriter.class);
        when(failingMapper.writerWithDefaultPrettyPrinter()).thenReturn(failingWriter);
        doAnswer(inv -> {
            throw mock(JsonProcessingException.class);
        }).when(failingWriter).writeValueAsString(any());

        LlmService serviceWithFailingMapper = new LlmService(chatClientBuilder, failingMapper);
        boolean serializationFailResult = serviceWithFailingMapper.isResume(createSampleResumeData());
        assertThat(serializationFailResult).isFalse();
    }

    @Test
    @DisplayName("Test 13: isResume(String) returns false for blank/null and evaluates non-blank text via LLM")
    @SuppressWarnings("unchecked")
    void isResume_StringText_ValidAndBlankInputs() {
        // Blank or null returns false immediately
        assertThat(llmService.isResume((String) null)).isFalse();
        assertThat(llmService.isResume("   ")).isFalse();

        // Valid resume text where LLM answers true
        when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .call()
                .content())
                .thenReturn("true");

        assertThat(llmService.isResume("John Doe - Work Experience - Java Developer")).isTrue();

        // LLM returns false
        when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .call()
                .content())
                .thenReturn("false");

        assertThat(llmService.isResume("Pancake recipe with flour and eggs")).isFalse();
    }

    @Test
    @DisplayName("Test 14: suggestChanges returns structured suggestions on valid inputs")
    @SuppressWarnings("unchecked")
    void suggestChanges_ValidInputs_ReturnsSuggestions() {
        // Given
        ResumeDataDto resume = createSampleResumeData();
        String jd = "Looking for Spring Boot engineer with microservices expertise";
        String role = "Senior Backend Engineer";
        String expectedSuggestion = "- Add quantitative metrics to projects\n- Highlight Spring Boot experience";

        when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .call()
                .content())
                .thenReturn(expectedSuggestion);

        // When
        String actualSuggestion = llmService.suggestChanges(resume, jd, role);

        // Then
        assertThat(actualSuggestion).isEqualTo(expectedSuggestion);
    }

    @Test
    @DisplayName("Test 15: suggestChanges returns null when serialization fails")
    void suggestChanges_JsonProcessingError_ReturnsNull() throws JsonProcessingException {
        // Given: ObjectWriter throwing JsonProcessingException via doAnswer
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        ObjectWriter failingWriter = mock(ObjectWriter.class);
        when(failingMapper.writerWithDefaultPrettyPrinter()).thenReturn(failingWriter);
        doAnswer(inv -> {
            throw mock(JsonProcessingException.class);
        }).when(failingWriter).writeValueAsString(any());

        LlmService serviceWithFailingMapper = new LlmService(chatClientBuilder, failingMapper);

        // When
        String result = serviceWithFailingMapper.suggestChanges(
                createSampleResumeData(),
                "Target JD",
                "Backend Engineer"
        );

        // Then
        assertThat(result).isNull();
    }
}