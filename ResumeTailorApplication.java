package one.harshit.resumeTailor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
public class ResumeTailorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeTailorApplication.class, args);
	}

}
