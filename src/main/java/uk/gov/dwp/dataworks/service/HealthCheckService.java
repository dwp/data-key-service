package uk.gov.dwp.dataworks.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kotlin.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.controller.HealthCheckController;
import uk.gov.dwp.dataworks.dto.HealthCheckResponse;
import uk.gov.dwp.dataworks.logging.DataworksLogger;

import static java.util.UUID.randomUUID;

@Service
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class HealthCheckService {
    private final HealthCheckController healthCheckController;
    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger("healthcheck");

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
            LOGGER.info("Healthcheck response",
                    new Pair("encryption_service", responseBody.get("encryptionService").asText()),
                    new Pair("master_key", responseBody.get("masterKey").asText()),
                    new Pair("datakey_generator", responseBody.get("dataKeyGenerator").asText()),
                    new Pair("encryption", responseBody.get("encryption").asText()),
                    new Pair("decryption", responseBody.get("decryption").asText()),
                    new Pair("correlation_id", correlationId));
        }
        else {
            LOGGER.error("Healthcheck response",
                    new Pair("encryption_service", responseBody.get("encryptionService").asText()),
                    new Pair("master_key", responseBody.get("masterKey").asText()),
                    new Pair("datakey_generator", responseBody.get("dataKeyGenerator").asText()),
                    new Pair("encryption", responseBody.get("encryption").asText()),
                    new Pair("decryption", responseBody.get("decryption").asText()),
                    new Pair("correlation_id", correlationId));
        }
    }
}
