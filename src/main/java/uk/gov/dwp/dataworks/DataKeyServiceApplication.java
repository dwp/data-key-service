package uk.gov.dwp.dataworks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@SpringBootApplication
@EnableWebSecurity
public class DataKeyServiceApplication extends WebSecurityConfigurerAdapter {

    private final static Logger LOGGER = LoggerFactory.getLogger(DataKeyServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DataKeyServiceApplication.class, args);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().authenticated().and().x509().subjectPrincipalRegex("CN=(.*?)(?:,|$)");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            LOGGER.info("Loading user '{}'.", username);
            return new User(username, "",
                    AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
        };
    }
}
