package uk.gov.dwp.dataworks.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.controller.HealthCheckController;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.dto.HealthCheckResponse;
import uk.gov.dwp.dataworks.errors.LoginException;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

@Service
public class HealthCheckService {
    private final HealthCheckController healthCheckController;
    private final static Logger LOGGER = LoggerFactory.getLogger("healthcheck");

    @Autowired
    public HealthCheckService(HealthCheckController healthCheckController) {
        this.healthCheckController = healthCheckController;
    }

    @Scheduled(initialDelay = 1000, fixedRateString = "${healthcheck.interval:6000}")
    public void logHealthCheck() throws JsonProcessingException {
        ResponseEntity<HealthCheckResponse> response = healthCheckController.healthCheck();

        JsonFactory factory = new MappingJsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        ObjectNode responseBody = mapper.valueToTree(response.getBody());
        responseBody.remove("trustedCertificates");
        if(response.getStatusCode().value() == 200) {
            LOGGER.info("HEALTHCHECK: {}", responseBody);
        }
        else {
            LOGGER.warn("HEALTHCHECK {}", responseBody);
        }
    }
}
