package uk.gov.dwp.dataworks.config;

import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("SECURE")
public class SecureConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests().antMatchers("/healthcheck").permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .x509()
                .subjectPrincipalRegex("CN=(.*?)(?:,|$)");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> new User(username, "",
                AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
    }


//    @Bean
//    public TomcatServletWebServerFactory servletContainer() {
//        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
//        Connector[] additionalConnectors = this.additionalConnector();
//        if (additionalConnectors != null && additionalConnectors.length > 0) {
//            tomcat.addAdditionalTomcatConnectors(additionalConnectors);
//        }
//        return tomcat;
//    }
//
//    private Connector[] additionalConnector() {
//        if (StringUtils.isBlank(this.additionalPorts)) {
//            return null;
//        }
//        String[] ports = this.additionalPorts.split(",");
//        List<Connector> result = new ArrayList<>();
//        for (String port : ports) {
//            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
//            connector.setScheme("http");
//            connector.setPort(Integer.valueOf(port));
//            result.add(connector);
//        }
//        return result.toArray(new Connector[] {});
//    }
//    @Bean
//    public EmbeddedServletContainerCustomizer containerCustomizer() {
//        return new EmbeddedServletContainerCustomizer() {
//            @Override
//            public void customize(ConfigurableEmbeddedServletContainer container) {
//                if (container instanceof TomcatEmbeddedServletContainerFactory) {
//                    TomcatEmbeddedServletContainerFactory containerFactory =
//                            (TomcatEmbeddedServletContainerFactory) container;
//
//                    Connector connector = new Connector(TomcatEmbeddedServletContainerFactory.DEFAULT_PROTOCOL);
//                    connector.setPort(httpPort);
//                    containerFactory.addAdditionalTomcatConnectors(connector);
//                }
//            }
//        };
//    }
//    @Value("${server.additionalPorts}")
//    private String additionalPorts;
}
