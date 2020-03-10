package uk.gov.dwp.dataworks.config;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class WebServerCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
            if (new File(crlFile).isFile()) {
                ((AbstractHttp11Protocol<?>) connector.getProtocolHandler()).setCrlFile(crlFile);
            }
        });
    }

    @Value("${server.ssl.crl.file:server.crl}")
    private String crlFile;
}
