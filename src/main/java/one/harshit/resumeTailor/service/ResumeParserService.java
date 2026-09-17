package one.harshit.resumeTailor.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import one.harshit.resumeTailor.model.ResumeDocument;
import one.harshit.resumeTailor.model.dto.ResumeDataDto;

import org.springframework.ai.document.Document;

@Service
public class ResumeParserService {

    private static Logger log = LoggerFactory.getLogger(ResumeParserService.class);

    private final ChatClient chatClient;

    ResumeParserService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public ResumeDataDto parseFile(ResumeDocument fileInfo) {
        Path filePath = fileInfo.getFilePath();
        String contentType = null;
        try {
            contentType = Files.probeContentType(filePath).replace("application/", "");
        } catch (IOException e) {
            log.error("Unable to get file type {}", e.getMessage());
            e.printStackTrace();
        }

        switch (contentType) {
            case "pdf":
                ResumeDataDto data=  parsePdfFormat(fileInfo);
                // String text = data.toString();
                // log.debug("Received text {}", text);
                return data;

            case "docx":
                return parsePdfFormat(fileInfo);
                

            default:
                log.warn("contentType could not be extracted for file: {}", filePath.getFileName());
                return null;

        }

    }

    // private ResumeDataDto parseDocxFormat(ResumeDocument fileInfo) {
    //     // TODO Auto-generated method stub
    //     throw new UnsupportedOperationException("Unimplemented method 'parseDocxFormat'");

    // }

    private ResumeDataDto parsePdfFormat(ResumeDocument fileInfo) {

        Path filePath = fileInfo.getFilePath();
        TikaDocumentReader  reader = new TikaDocumentReader(new FileSystemResource(filePath));
        
        List<Document> documents = reader.get();

        String resumeText = documents.stream().map(doc -> doc.getText()).collect(Collectors.joining("\n"));

        return chatClient.prompt().system(
                "You are an expert resume parser. Extract structured details from the resume text accurately into the requested JSON schema.")
                .user(u -> u.text("Extract the resume details from the following text: \n \n{text}").param("text",
                        resumeText))
                .call().entity(ResumeDataDto.class);

    }

}
