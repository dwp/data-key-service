package uk.gov.dwp.dataworks.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.controller.HealthCheckController;
import uk.gov.dwp.dataworks.dto.HealthCheckResponse;
import static java.util.UUID.randomUUID;

@Service
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class HealthCheckService {
    private final HealthCheckController healthCheckController;
    private final static Logger LOGGER = LoggerFactory.getLogger("healthcheck");

    @Autowired
    public HealthCheckService(HealthCheckController healthCheckController) {
        this.healthCheckController = healthCheckController;
    }

    @Scheduled(initialDelay = 1000, fixedRateString = "${healthcheck.interval:10000}")
    public void logHealthCheck() {
        String correlationId = randomUUID().toString();
        ResponseEntity<HealthCheckResponse> response = healthCheckController.healthCheck(correlationId);
        JsonFactory factory = new MappingJsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        ObjectNode responseBody = mapper.valueToTree(response.getBody());
        responseBody.remove("trustedCertificates");
        if(response.getStatusCode().value() == 200) {
            LOGGER.info("HEALTHCHECK: {}, dks_correlation_id: {}", responseBody, correlationId);
        }
        else {
            LOGGER.warn("HEALTHCHECK: {}, dks_correlation_id: {}", responseBody, correlationId);
        }
    }
}
