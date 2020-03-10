package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.cfm2.CFM2Exception;
import com.cavium.cfm2.LoginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dwp.dataworks.dto.HSMCredentials;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.provider.HsmLoginManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.dwp.dataworks.provider.hsm.HsmDataKeyDecryptionConstants.MAX_ATTEMPTS;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"UnitTest", "HSM", "ExplicitHSMLogin"})
@TestPropertySource(properties = {"server.environment_name=development",
        "cache.eviction.interval=1000",
        "scheduling.enabled=false"
})
public class HSMLoginManagerTest {

    private static final String CRYPTO_USER = "development_crypto_user";
    private static final String CRYPTO_USER_PASSWORD = CRYPTO_USER + ".password";
    private static final String CRYPTO_USER_PARTITION_ID = "partition_id";

    @MockBean
    private HsmCredentialsProvider hsmCredentialsProvider;

    @Autowired
    private HsmLoginManager hsmLoginManager;

    @MockBean
    private LoginManager loginManager;

    @Before
    public void init() {
        Mockito.reset(hsmCredentialsProvider);
    }

    static CFM2Exception dummyC2smException() {
        return new CFM2Exception(1610612865, "dummy-call");
    }

    @Test
    public void Should_Login_When_Credentials_Are_Not_Null() throws CFM2Exception, MasterKeystoreException {
        HSMCredentials hsmCredentials = new HSMCredentials(CRYPTO_USER, CRYPTO_USER_PASSWORD, CRYPTO_USER_PARTITION_ID);
        given(hsmCredentialsProvider.getCredentials()).willReturn(hsmCredentials);
        doNothing().when(loginManager).login(CRYPTO_USER_PARTITION_ID, CRYPTO_USER, CRYPTO_USER_PASSWORD);

        hsmLoginManager.login();

        verify(loginManager, Mockito.times(1)).login(CRYPTO_USER_PARTITION_ID, CRYPTO_USER, CRYPTO_USER_PASSWORD);
    }

    @Test
    public void Should_retry_when_login_fails() throws CFM2Exception, MasterKeystoreException {
        try {
            HSMCredentials hsmCredentials = new HSMCredentials(CRYPTO_USER, CRYPTO_USER_PASSWORD, CRYPTO_USER_PARTITION_ID);
            given(hsmCredentialsProvider.getCredentials()).willReturn(hsmCredentials);
            doThrow(dummyC2smException()).when(loginManager).login(CRYPTO_USER_PARTITION_ID, CRYPTO_USER, CRYPTO_USER_PASSWORD);

            hsmLoginManager.login();
        } catch (MasterKeystoreException ex) {
            verify(loginManager, Mockito.times(MAX_ATTEMPTS)).login(CRYPTO_USER_PARTITION_ID, CRYPTO_USER, CRYPTO_USER_PASSWORD);
            assertEquals("Failed to login, will retry (unless '" + MAX_ATTEMPTS + "' attempts made).", ex.getMessage());
        }
    }

    @Test
    public void Should_retry_until_login_succeeds_when_error_occurs() throws CFM2Exception, MasterKeystoreException {
        HSMCredentials hsmCredentials = new HSMCredentials(CRYPTO_USER, CRYPTO_USER_PASSWORD, CRYPTO_USER_PARTITION_ID);
        given(hsmCredentialsProvider.getCredentials()).willReturn(hsmCredentials);
        doThrow(dummyC2smException())
                .doNothing()
                .when(loginManager)
                .login(CRYPTO_USER_PARTITION_ID, CRYPTO_USER, CRYPTO_USER_PASSWORD);

        hsmLoginManager.login();

        verify(loginManager, Mockito.times(2)).login(CRYPTO_USER_PARTITION_ID, CRYPTO_USER, CRYPTO_USER_PASSWORD);
    }

    @Test
    public void Should_Logout_When_Logout_Is_Invoked() throws CFM2Exception, MasterKeystoreException {
        doNothing().when(loginManager).logout();
        hsmLoginManager.logout();
        verify(loginManager, Mockito.times(1)).logout();
    }
}
