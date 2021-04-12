package uk.gov.dwp.dataworks.provider.hsm;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.dto.HSMCredentials;
import uk.gov.dwp.dataworks.logging.DataworksLogger;
import uk.gov.dwp.dataworks.provider.HsmLoginManager;

@Component
@Profile("ImplicitHSMLogin")
public class ImplicitHsmLoginManager implements HsmLoginManager {
    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(ImplicitHsmLoginManager.class.toString());

    private final HsmCredentialsProvider hsmCredentialsProvider;

    public ImplicitHsmLoginManager(HsmCredentialsProvider hsmCredentialsProvider) {
        this.hsmCredentialsProvider = hsmCredentialsProvider;
    }

    @Override
    public void login() {
        HSMCredentials hsmCredentials = hsmCredentialsProvider.getCredentials();
        if (null != hsmCredentials) {
            LOGGER.info("Setting implicit login details");
            System.setProperty("HSM_PARTITION", hsmCredentials.getPartitionId());
            System.setProperty("HSM_USER", hsmCredentials.getUserName());
            System.setProperty("HSM_PASSWORD", hsmCredentials.getPassWord());
        }
    }
}
