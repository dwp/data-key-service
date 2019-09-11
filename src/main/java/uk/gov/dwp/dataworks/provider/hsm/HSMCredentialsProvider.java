package uk.gov.dwp.dataworks.provider.hsm;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.HSMCredentials;

@Service
@Profile("HSM")
public class HSMCredentialsProvider {

    private static final String CRYPTO_USER = "_crypto_user";
    private static final String CRYPTO_USER_PASSWORD = CRYPTO_USER + ".password";
    private static final String CRYPTO_USER_CLUSTERID = "cluster_id";
    private static final String HSM_CREDENTIALS_CACHE_NAME = "hsmcredentials";

    private final AWSSimpleSystemsManagement awsSimpleSystemsManagementClient;
    private final static Logger LOGGER = LoggerFactory.getLogger(HSMCredentialsProvider.class);

    @Autowired
    public HSMCredentialsProvider(AWSSimpleSystemsManagement awsSimpleSystemsManagementClient) {
        this.awsSimpleSystemsManagementClient = awsSimpleSystemsManagementClient;
    }

    @Value("${server.environment_name}")
    private String environmentName;

    @Scheduled(fixedRateString = "${cache.eviction.interval:120000}")
    @CacheEvict(value = HSM_CREDENTIALS_CACHE_NAME, allEntries = true)
    public void clearCache() {
        LOGGER.info("Cache evicted");

    }

    @Cacheable(value = HSM_CREDENTIALS_CACHE_NAME, key = "#root.methodName")
    public HSMCredentials getCredentials() {
        HSMCredentials hsmCredentials = null;
        try {
            if (null != environmentName) {
                String username = environmentName + CRYPTO_USER;
                GetParameterRequest pwdRequest = new GetParameterRequest()
                        .withName(environmentName + CRYPTO_USER_PASSWORD)
                        .withWithDecryption(true);
                String password = awsSimpleSystemsManagementClient.getParameter(pwdRequest).getParameter().getValue();

                GetParameterRequest clusterIdRequest = new GetParameterRequest()
                        .withName(CRYPTO_USER_CLUSTERID)
                        .withWithDecryption(true);
                String clusterId = awsSimpleSystemsManagementClient.getParameter(clusterIdRequest).getParameter().getValue();

                hsmCredentials = new HSMCredentials(username, password, clusterId);
            } else {
                LOGGER.error("server.environment_name property is null");
            }

        } catch (RuntimeException e) {
            LOGGER.error("Failed to retrieve the HSM credentials.", e);
        }
        return hsmCredentials;
    }
}