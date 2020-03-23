package uk.gov.dwp.dataworks.provider.hsm;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.HSMCredentials;
import uk.gov.dwp.dataworks.errors.LoginException;
import uk.gov.dwp.dataworks.logging.DataworksLogger;

@Service
@Profile("HSM")
public class HsmCredentialsProvider implements HsmDataKeyDecryptionConstants {

    private static final String CRYPTO_USER = "_crypto_user";
    private static final String CRYPTO_USER_PASSWORD = CRYPTO_USER + ".password";
    private static final String CRYPTO_USER_PARTITION_ID = "hsm_partitionid";
    private static final String HSM_CREDENTIALS_CACHE_NAME = "hsmcredentials";

    private final AWSSimpleSystemsManagement awsSimpleSystemsManagementClient;
    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(HsmCredentialsProvider.class.toString());

    @Autowired
    public HsmCredentialsProvider(AWSSimpleSystemsManagement awsSimpleSystemsManagementClient) {
        this.awsSimpleSystemsManagementClient = awsSimpleSystemsManagementClient;
    }

    @Value("${server.environment_name}")
    private String environmentName;

    @Scheduled(fixedRateString = "${credentials.cache.eviction.interval:120000}")
    @CacheEvict(value = HSM_CREDENTIALS_CACHE_NAME, allEntries = true)
    public void clearCache() {
        LOGGER.debug("Credentials cache evicted.");
    }

    @Cacheable(value = HSM_CREDENTIALS_CACHE_NAME, key = "#root.methodName")
    public HSMCredentials getCredentials() {
        HSMCredentials hsmCredentials;
        try {
            if (!Strings.isNullOrEmpty(environmentName)) {
                String username = environmentName + CRYPTO_USER;
                GetParameterRequest pwdRequest = new GetParameterRequest()
                        .withName(environmentName + CRYPTO_USER_PASSWORD)
                        .withWithDecryption(true);
                String password = awsSimpleSystemsManagementClient.getParameter(pwdRequest).getParameter().getValue();

                GetParameterRequest partitionIdRequest = new GetParameterRequest()
                        .withName(CRYPTO_USER_PARTITION_ID)
                        .withWithDecryption(true);
                String partitionId = awsSimpleSystemsManagementClient.getParameter(partitionIdRequest).getParameter().getValue();

                if (!(Strings.isNullOrEmpty(password) || Strings.isNullOrEmpty(partitionId))) {
                    hsmCredentials = new HSMCredentials(username, password, partitionId);
                } else {
                    LOGGER.error("Either username or password is null or empty");
                    throw new LoginException("Either username or password is null or empty");
                }
            } else {
                LOGGER.error("server.environment_name property is null");
                throw new LoginException("Unknown environment");
            }

        } catch (Exception e) {
            String message = "Failed to retrieve the HSM credentials";
            LOGGER.error(message, e);
            throw new LoginException(message + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return hsmCredentials;
    }
}
