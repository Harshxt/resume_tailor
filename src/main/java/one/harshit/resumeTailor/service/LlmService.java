package one.harshit.resumeTailor.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.databind.ObjectMapper;

import one.harshit.resumeTailor.model.dto.ResumeDataDto;

@Service
public class LlmService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private static Logger log = LoggerFactory.getLogger(LlmService.class);

    LlmService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String suggestChanges(ResumeDataDto resumeData, String targetJd, String targetRole) {
        final String SYSTEM_PROMPT_WRITER = "You are an expert resume writer. Suggest changes to the resume based on the provided structured resume data. Do not use generic suggestions. Be very specific regarding the changes the suggestions to the target Job Description and job role if provided. Mention what is missing and what has to be refactored. Also mention";
        try {
            String resumeJson = serializeDto(resumeData);
            Map<String, Object> params = new HashMap<>();
            params.put("resumeJson", resumeJson);
            params.put("targetJd", targetJd);
            params.put("targetRole", targetRole);
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT_WRITER)
                    .user(u -> u.text(
                            "Here is the candidate's structured resume data:\n```json\n{resumeJson}\n```\n\n for the following Job Description:\n```json\n{targetJd}\n```\n\n and the target role:\n```json\n{targetRole}\n```\n\n Please suggest impactful changes and improvements.")
                            .params(params))
                    .call()
                    .content(); // swap with ChatResponse in the future to add usage logging
            log.debug("Response from suggestChanges : {}", response);
            return response;
        } catch (JsonProcessingException e) {
            log.error("Could not proocess the JSON : {}", e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private String serializeDto(Record record) throws JsonProcessingException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(record);

        return json;
    }

    public boolean isResume(ResumeDataDto resumeData) {
        if (resumeData == null) {
            return false;
        }

        try {
            String resumeJson = serializeDto(resumeData);
            final String SYSTEM_PROMPT_VERIFIER = """
                    You are an expert document classifier.
                    Determine whether the provided structured data represents a genuine, valid resume or CV.
                    Respond with only 'true' if it is a resume/CV, or 'false' if it is not.
                    Do not include any other markdown, explanation, or additional text.
                    """;

            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT_VERIFIER)
                    .user(u -> u.text(
                            "Analyze the following structured data and determine if it represents a resume:\n```json\n{resumeJson}\n```")
                            .param("resumeJson", resumeJson))
                    .call()
                    .content();

            log.debug("Response from isResume(ResumeDataDto) : {}", response);
            if (response == null) {
                return false;
            }
            String cleaned = response.trim().toLowerCase();
            return cleaned.contains("true") && !cleaned.contains("false");
        } catch (JsonProcessingException e) {
            log.error("Could not serialize ResumeDataDto: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Failed to verify if ResumeDataDto is a resume: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean isResume(String documentText) {
        if (documentText == null || documentText.isBlank()) {
            return false;
        }

        final String SYSTEM_PROMPT_VERIFIER = """
                You are an expert document classifier.
                Determine whether the provided text content belongs to a resume or CV.
                Respond with only 'true' if it is a resume/CV, or 'false' if it is not.
                Do not include any other markdown, explanation, or additional text.
                """;

        try {
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT_VERIFIER)
                    .user(u -> u
                            .text("Analyze the following document content and determine if it is a resume:\n\n{text}")
                            .param("text", documentText))
                    .call()
                    .content();

            log.debug("Response from isResume : {}", response);
            if (response == null) {
                return false;
            }
            String cleaned = response.trim().toLowerCase();
            return cleaned.contains("true") && !cleaned.contains("false");
        } catch (Exception e) {
            log.error("Failed to verify if document is a resume: {}", e.getMessage(), e);
            return false;
        }
    }

    public String debloatJobDescription(String jobDescription) {
        final String SYSTEM_PROMPT = """
                You are an expert technical recruiter and resume-tailoring assistant.
                Your task is to debloat the provided job description to retain only resume-relevant details.
                CRITICAL RULES:
                1. First, check if the input is a genuine job description or posting.
                   - If it is NOT a job description, return an EMPTY STRING (""). Absolutely no commentary, greetings, or explanations.
                2. If it IS a job description:
                   - Summarize company details (industry, domain, core product) into 2-3 concise sentences.
                   - Retain role title, core responsibilities, qualifications, required/preferred technical skills, and tools.
                   - STRIP OUT all salary/pay/compensation, perks/benefits, EEO/legal disclaimers, visa sponsorship notes, and application/recruiter instructions.
                   - Return strictly the structured markdown output with NO intro/outro conversational text.
                """;

        try {
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.text("Input Text: \n \n {text}").param("text", jobDescription))
                    .call().content();

            return response != null ? response.trim() : "";

        } catch (Exception e) {
            log.error("Failed to debloat job description: {}", e.getMessage(), e);
            return "";
        }

    }

}
