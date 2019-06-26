package uk.gov.dwp.dataworks.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
@Profile("SECURE")
public class SecureConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests().anyRequest().authenticated()
                .and()
                .x509().subjectPrincipalRegex("CN=(.*?)(?:,|$)");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            LOGGER.info("Loading user '{}'.", username);
            return new User(username, "",
                    AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
        };
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(SecureConfiguration.class);
}
