package one.harshit.resumeTailor.controller;

import one.harshit.resumeTailor.service.StorageService;

import org.apache.poi.ss.formula.functions.T;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.node.ObjectNode;

import one.harshit.resumeTailor.controller.dto.GenericResponse;
import one.harshit.resumeTailor.model.ResumeDocument;
import one.harshit.resumeTailor.service.ResumeParserService;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/resume")
public class Controller {

    private final StorageService storageService;
    private final ResumeParserService resumeParserService;
    final ObjectMapper objectMapper;

    Controller(ResumeParserService resumeParserService, ObjectMapper objectMapper, StorageService storageService) {
        this.resumeParserService = resumeParserService;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
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
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new GenericResponse<T>(false, "Uploaded file is empty"));
        }


        String filename = file.getOriginalFilename();

        ResumeDocument document = storageService.store(file);

        resumeParserService.parseFile(document);

        


        ObjectNode json = objectMapper.createObjectNode();
        json.put("jobDescription", data.jobDescription);
        json.put("targetRole", data.targetRole);

        

        return ResponseEntity.ok(new GenericResponse<>(true, "Received file: "+ filename, json));

    }
}