package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.cfm2.CFM2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.dto.HSMCredentials;
import uk.gov.dwp.dataworks.errors.LoginException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.provider.HsmLoginManager;

@Component
@Profile("ExplicitHSMLogin")
public class ExplicitHsmLoginManager implements HsmLoginManager, HsmDataKeyDecryptionConstants {
    private final static Logger LOGGER = LoggerFactory.getLogger(ExplicitHsmLoginManager.class);

    @Autowired
    private com.cavium.cfm2.LoginManager loginManager;

    @Autowired
    private HsmCredentialsProvider hsmCredentialsProvider;

    @Override
    @Retryable(
            value = {MasterKeystoreException.class},
            maxAttempts = MAX_ATTEMPTS,
            backoff = @Backoff(delay = INITIAL_BACKOFF_MILLIS, multiplier = BACKOFF_MULTIPLIER))
    public void login() throws MasterKeystoreException {
        try {
            HSMCredentials hsmCredentials = hsmCredentialsProvider.getCredentials();
            if (null != hsmCredentials) {
                loginManager.login(hsmCredentials.getPartitionId(), hsmCredentials.getUserName(), hsmCredentials.getPassWord());

            }
        } catch (CFM2Exception e) {
            String message = "Failed to login, will retry (unless '" + MAX_ATTEMPTS + "' attempts made).";
            LOGGER.warn(message);
            throw new MasterKeystoreException(message, e);
        }
    }

    @Override
    @Retryable(
            value = {MasterKeystoreException.class},
            maxAttempts = MAX_ATTEMPTS,
            backoff = @Backoff(delay = INITIAL_BACKOFF_MILLIS, multiplier = BACKOFF_MULTIPLIER))
    public void logout() throws LoginException, MasterKeystoreException {
        try {
            loginManager.logout();
        } catch (CFM2Exception e) {
            String message = "Failed to logout, will retry (unless '" + MAX_ATTEMPTS + "' attempts made).";
            LOGGER.warn(message);
            throw new MasterKeystoreException(message, e);
        }
    }
}
