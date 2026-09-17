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

}
