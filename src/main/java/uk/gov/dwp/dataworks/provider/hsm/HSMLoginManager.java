package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.cfm2.CFM2Exception;
import com.cavium.cfm2.LoginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.dto.HSMCredentials;
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
    public  void login() {

        HSMCredentials hsmCredentials = hsmCredentialsProvider.getCredentials();
        try {
            if (null != hsmCredentials) {
                loginManager.login(hsmCredentials.getClusterId(), hsmCredentials.getUserName(), hsmCredentials.getPassWord());
                LOGGER.info("Login successful!");
            }
        } catch (CFM2Exception e) {
            if (CFM2Exception.isAuthenticationFailure(e)) {
                LOGGER.error("Detected invalid credentials");
            }
        }
    }

    @Override
    public  void logout() {

        try {
            loginManager.logout();
            LOGGER.info("Logout successful!");
        } catch (CFM2Exception e) {
                LOGGER.error("Detected invalid credentials");
        }
    }
}
