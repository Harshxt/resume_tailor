package one.harshit.resumeTailor.controller;

import one.harshit.resumeTailor.service.LlmService;
import one.harshit.resumeTailor.service.StorageService;

import java.util.concurrent.CompletableFuture;

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
import one.harshit.resumeTailor.model.dto.ResumeMatchResultDto;
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

    private record ParsedResumeContext(String filename, ResumeDataDto resumeData, String sanitisedJd,
            String targetRole) {
    }

    private ParsedResumeContext prepareContext(MultipartFile file, TailorRequest data) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        ResumeDocument document = storageService.store(file);
        ResumeDataDto resumeData = resumeParserService.parseFile(document);
        if (!llmService.isResume(resumeData)) {
            throw new IllegalArgumentException("The document provided is not a resume");
        }
        String sanitisedJd = llmService.debloatJobDescription(data != null ? data.jobDescription() : "");
        String targetRole = data != null ? data.targetRole() : "";
        return new ParsedResumeContext(file.getOriginalFilename(), resumeData, sanitisedJd, targetRole);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResume(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "data", required = false) TailorRequest data) {
        log.debug("TARGET DESCRIPTION: {}", data.jobDescription());
        log.debug("TARGET ROLE: {}", data.targetRole());

        ParsedResumeContext ctx = prepareContext(file, data);

        CompletableFuture<ResumeMatchResultDto> scoreFuture = CompletableFuture.supplyAsync(
                () -> llmService.calculateMatchScore(ctx.resumeData(), ctx.sanitisedJd(), ctx.targetRole()));

        CompletableFuture<String> suggestionsFuture = CompletableFuture
                .supplyAsync(() -> llmService.suggestChanges(ctx.resumeData(), ctx.sanitisedJd(), ctx.targetRole()));

        ObjectNode json = objectMapper.createObjectNode();

        json.put("suggestions", suggestionsFuture.join());
        json.set("matchScores", objectMapper.valueToTree(scoreFuture.join()));

        return ResponseEntity.ok(new GenericResponse<>(true, "Analysis completed for: " + ctx.filename(), json));

    }

    @PostMapping(value = "/match-score", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> getMatchScore(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "data", required = false) TailorRequest data) {
        ParsedResumeContext ctx = prepareContext(file, data);
        ResumeMatchResultDto score = llmService.calculateMatchScore(ctx.resumeData(), ctx.sanitisedJd(),
                ctx.targetRole());
        return ResponseEntity.ok(new GenericResponse<>(true, "Score calculated", score));
    }

    // check to see if the server is up
    @GetMapping("/health")
    public String healthCheckString() {
        log.debug("all good");
        return "All good";
    }

}