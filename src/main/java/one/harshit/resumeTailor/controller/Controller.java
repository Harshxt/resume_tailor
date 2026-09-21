package one.harshit.resumeTailor.controller;

import one.harshit.resumeTailor.service.LlmService;
import one.harshit.resumeTailor.service.StorageService;

import org.apache.poi.ss.formula.functions.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.node.ObjectNode;

import one.harshit.resumeTailor.controller.dto.GenericResponse;
import one.harshit.resumeTailor.model.ResumeDocument;
import one.harshit.resumeTailor.model.dto.ResumeDataDto;
import one.harshit.resumeTailor.service.ResumeParserService;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/resume")
public class Controller {

    private final LlmService llmService;
    private final StorageService storageService;
    private final ResumeParserService resumeParserService;
    final ObjectMapper objectMapper;
    private final Logger log = LoggerFactory.getLogger(Controller.class);

    Controller(ResumeParserService resumeParserService, ObjectMapper objectMapper, StorageService storageService,
            LlmService llmService) {
        this.resumeParserService = resumeParserService;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.llmService = llmService;
    }

    // Request DTO for structured JSON data (can be placed in a separate file)
    public record TailorRequest(
            String jobDescription,
            String targetRole
    // Add any additional JSON fields here
    ) {
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResume(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "data", required = false) TailorRequest data) {
        log.debug("TARGET DESCRIPTION: {}", data.jobDescription());
        log.debug("TARGET ROLE: {}", data.targetRole());
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new GenericResponse<T>(false, "Uploaded file is empty"));
        }

        String filename = file.getOriginalFilename();

        ResumeDocument document = storageService.store(file);

        ResumeDataDto resumeData = resumeParserService.parseFile(document);

        boolean isResume = llmService.isResume(resumeData);
        
        if (!isResume) {
            return ResponseEntity.badRequest()
                    .body(new GenericResponse<>(false, "The document provided is not a resume"));
        }
        String santisedJobDescription = llmService.debloatJobDescription(data.jobDescription());
        // calling the llm service to request for suggestion
        String suggestion = llmService.suggestChanges(resumeData, santisedJobDescription, data.targetRole());

        ObjectNode json = objectMapper.createObjectNode();
        json.put("suggestions", suggestion);

        return ResponseEntity.ok(new GenericResponse<>(true, "Received file: " + filename, json));

    }

    @GetMapping("/health")
    public String healthCheckString() {
        log.debug("all good");
        return "All good";
    }



}