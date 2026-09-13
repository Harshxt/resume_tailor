package one.harshit.resumeTailor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication //(exclude = {SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
public class ResumeTailorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeTailorApplication.class, args);
	}

}
