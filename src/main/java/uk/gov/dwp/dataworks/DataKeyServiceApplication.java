package uk.gov.dwp.dataworks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableWebSecurity
@EnableScheduling
@EnableCaching
@EnableRetry
public class DataKeyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataKeyServiceApplication.class, args);
    }

}
