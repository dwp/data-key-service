package uk.gov.dwp.dataworks.provider.hsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.dto.HSMCredentials;
import uk.gov.dwp.dataworks.provider.HsmLoginManager;

@Component
@Profile("ImplicitHSMLogin")
public class ImplicitHsmLoginManager implements HsmLoginManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(ImplicitHsmLoginManager.class);

    @Autowired
    private com.cavium.cfm2.LoginManager loginManager ;

    @Autowired
    private HsmCredentialsProvider hsmCredentialsProvider;

    @Override
    public  void login() {
        HSMCredentials hsmCredentials = hsmCredentialsProvider.getCredentials();
        if (null != hsmCredentials) {
            LOGGER.info("Setting implicit login details");
            System.setProperty("HSM_PARTITION", hsmCredentials.getPartitionId());
            System.setProperty("HSM_USER", hsmCredentials.getUserName());
            System.setProperty("HSM_PASSWORD", hsmCredentials.getPassWord());
        }
    }


    @Override
    public  void logout() {
    }
}
