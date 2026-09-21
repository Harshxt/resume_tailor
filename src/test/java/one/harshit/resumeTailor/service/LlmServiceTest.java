package one.harshit.resumeTailor.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.test.context.SpringBootTest;

import picocli.CommandLine.Help.Ansi;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class LlmServiceTest {

    @Autowired
    private LlmService llmService;

    @Test
    void testDebloatValidJobDescription() {
        String sampleJd = """
                About ACME Corp:
                ACME is a leading fintech platform processing $10B annually in payments.
                We value innovation, high standards, and transparency.

                Position: Senior Backend Engineer
                Compensation: $160,000 - $190,000 + 401(k) matching and dental insurance.
                Location: Remote (US only).

                Responsibilities:
                - Design and build scalable REST and gRPC microservices in Java/Spring Boot.
                - Optimize PostgreSQL database queries and manage Redis caching.
                - Collaborate with frontend engineers and product managers.

                Requirements:
                - 5+ years of experience with Java and Spring Boot.
                - Strong experience with relational databases (PostgreSQL/MySQL).
                - Experience with Docker and AWS.

                Equal Opportunity Employer:
                ACME Corp provides equal employment opportunities to all applicants regardless of race, color, religion...
                To apply, email your resume to jobs@acme.example.com.
                """;

        String result = llmService.debloatJobDescription(sampleJd);
        System.out.println("=== DEBLOATED JD OUTPUT ===");
        System.out.println(result);
        System.out.println("===========================");

        assertThat(result).isNotBlank();
        assertThat(result).doesNotContain("$160,000");
        assertThat(result).doesNotContain("Equal Opportunity Employer");
    }

    @Test
    void testDebloatNonJobDescriptionReturnsEmpty() {
        String randomText = "Today the weather is sunny and I had pancakes for breakfast.";
        String result = llmService.debloatJobDescription(randomText);
        System.out.println("=== INVALID JD RESULT ===");
        System.out.println("\u001B[33mRESULT: '" + "'\u001B[0m" + result );
        System.out.println("=========================");
        assertThat(result).isBlank();
    }
}