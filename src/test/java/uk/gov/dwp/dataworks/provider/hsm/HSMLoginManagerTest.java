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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"UnitTest", "HSM", "ExplicitHSMLogin"})
@TestPropertySource(properties = {"server.environment_name=development",
        "cache.eviction.interval=1000"
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

    @Test
    public void Should_Login_When_Credentials_Are_Not_Null() throws CFM2Exception, MasterKeystoreException {
        HSMCredentials hsmCredentials = new HSMCredentials(CRYPTO_USER,CRYPTO_USER_PASSWORD, CRYPTO_USER_PARTITION_ID);
        given(hsmCredentialsProvider.getCredentials()).willReturn(hsmCredentials);
        hsmLoginManager.login();
        doNothing().when(loginManager).login(CRYPTO_USER_PARTITION_ID,CRYPTO_USER,CRYPTO_USER_PASSWORD);
        verify(loginManager,Mockito.times(1)).login(CRYPTO_USER_PARTITION_ID,CRYPTO_USER,CRYPTO_USER_PASSWORD);
    }

    @Test(expected = MasterKeystoreException.class)
    public void Should_retry_when_login_fails() throws CFM2Exception, MasterKeystoreException {
        HSMCredentials hsmCredentials = new HSMCredentials(CRYPTO_USER,CRYPTO_USER_PASSWORD, CRYPTO_USER_PARTITION_ID);
        given(hsmCredentialsProvider.getCredentials()).willReturn(hsmCredentials);
        doThrow(CFM2Exception.class).when(loginManager).login(CRYPTO_USER_PARTITION_ID,CRYPTO_USER,CRYPTO_USER_PASSWORD);
        hsmLoginManager.login();
        verify(loginManager,Mockito.times(10)).login(CRYPTO_USER_PARTITION_ID,CRYPTO_USER,CRYPTO_USER_PASSWORD);
    }

    @Test
    public void Should_retry_until_login_succeeds() throws CFM2Exception, MasterKeystoreException {
        HSMCredentials hsmCredentials = new HSMCredentials(CRYPTO_USER,CRYPTO_USER_PASSWORD, CRYPTO_USER_PARTITION_ID);
        given(hsmCredentialsProvider.getCredentials()).willReturn(hsmCredentials);
        doThrow(CFM2Exception.class)
                .doNothing()
                .when(loginManager)
                .login(CRYPTO_USER_PARTITION_ID,CRYPTO_USER,CRYPTO_USER_PASSWORD);
        hsmLoginManager.login();
        verify(loginManager,Mockito.times(2)).login(CRYPTO_USER_PARTITION_ID,CRYPTO_USER,CRYPTO_USER_PASSWORD);
    }

    @Test
    public void Should_Logout_When_Logout_Is_Invoked() throws CFM2Exception, MasterKeystoreException {
        doNothing().when(loginManager).logout();
        hsmLoginManager.logout();
        verify(loginManager,Mockito.times(1)).logout();
    }
}