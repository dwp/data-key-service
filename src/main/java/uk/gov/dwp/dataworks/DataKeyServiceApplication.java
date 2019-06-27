package uk.gov.dwp.dataworks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableWebSecurity
public class DataKeyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataKeyServiceApplication.class, args);
    }

}
