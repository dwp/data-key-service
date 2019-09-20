package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.cfm2.CFM2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.dto.HSMCredentials;
import uk.gov.dwp.dataworks.errors.LoginException;
import uk.gov.dwp.dataworks.provider.LoginManager;

@Component
@Profile("HSM")
public class HsmLoginManager implements LoginManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(HsmLoginManager.class);

    @Autowired
    private com.cavium.cfm2.LoginManager loginManager ;

    @Autowired
    private HsmCredentialsProvider hsmCredentialsProvider;

    @Override
    public  void login() throws LoginException {
        try {
            HSMCredentials hsmCredentials = hsmCredentialsProvider.getCredentials();
            if (null != hsmCredentials) {
                loginManager.login(hsmCredentials.getPartitionId(), hsmCredentials.getUserName(), hsmCredentials.getPassWord());
            }
        }
        catch (CFM2Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new LoginException(e);
        }
    }

    @Override
    public  void logout() throws LoginException {
        try {
            loginManager.logout();
        }
        catch (CFM2Exception e) {
            throw new LoginException(e);
        }
    }
}
