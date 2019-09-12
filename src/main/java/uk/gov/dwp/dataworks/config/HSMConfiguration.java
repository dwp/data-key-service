package uk.gov.dwp.dataworks.config;

import com.cavium.cfm2.LoginManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("HSM")
public class HSMConfiguration {

    @Bean
    LoginManager loginManager() {
        return LoginManager.getInstance();
    }
}
