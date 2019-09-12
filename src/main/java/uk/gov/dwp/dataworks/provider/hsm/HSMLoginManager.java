package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.cfm2.CFM2Exception;
import com.cavium.cfm2.LoginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.dto.HSMCredentials;
import uk.gov.dwp.dataworks.errors.LoginException;
import uk.gov.dwp.dataworks.provider.aws.AWSLoginManager;

@Component
@Profile("HSM")
public class HSMLoginManager implements AWSLoginManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(HSMLoginManager.class);

    @Autowired
    private LoginManager loginManager ;

    @Autowired
    private HSMCredentialsProvider hsmCredentialsProvider;

    @Override
    public  void login() throws LoginException {
        try {
            HSMCredentials hsmCredentials = hsmCredentialsProvider.getCredentials();
            if (null != hsmCredentials) {
                loginManager.login(hsmCredentials.getClusterId(), hsmCredentials.getUserName(), hsmCredentials.getPassWord());
            }
        }
        catch (CFM2Exception e) {
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
