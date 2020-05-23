package uk.gov.dwp.dataworks.config;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class WebServerCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers((connector) -> {
            ((AbstractHttp11Protocol<?>) connector.getProtocolHandler())
                    .setCrlFile("/home/danielchicot/src/dwp/data-key-service/resources/development.crl");

            ((AbstractHttp11Protocol<?>) connector.getProtocolHandler()).reloadSslHostConfigs();
        });
    }


}
