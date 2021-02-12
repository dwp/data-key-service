package uk.gov.dwp.dataworks;

import io.prometheus.client.spring.web.EnablePrometheusTiming;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableCaching
@EnablePrometheusTiming
@EnableRetry
@EnableScheduling
@EnableWebSecurity
@SpringBootApplication
public class DataKeyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataKeyServiceApplication.class, args);
    }
}
