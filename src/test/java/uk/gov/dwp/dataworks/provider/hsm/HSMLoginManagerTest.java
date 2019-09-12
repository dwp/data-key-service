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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"UnitTest", "HSM"})
@TestPropertySource(properties = {"server.environment_name=development",
        "cache.eviction.interval=1000"
})
public class HSMLoginManagerTest {

    private static final String CRYPTO_USER = "development_crypto_user";
    private static final String CRYPTO_USER_PASSWORD = CRYPTO_USER + ".password";
    private static final String CRYPTO_USER_CLUSTERID = "cluster_id";

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
    public void Should_Login_When_Credentials_Are_Not_Null() throws CFM2Exception {
        HSMCredentials hsmCredentials = new HSMCredentials(CRYPTO_USER,CRYPTO_USER_PASSWORD,CRYPTO_USER_CLUSTERID);
        given(hsmCredentialsProvider.getCredentials()).willReturn(hsmCredentials);
        hsmLoginManager.login();
        doNothing().when(loginManager).login(CRYPTO_USER_CLUSTERID,CRYPTO_USER,CRYPTO_USER_PASSWORD);
        verify(loginManager,Mockito.times(1)).login(CRYPTO_USER_CLUSTERID,CRYPTO_USER,CRYPTO_USER_PASSWORD);
    }

    @Test
    public void Should_Logout_When_Logout_Is_Invoked() throws CFM2Exception {
        doNothing().when(loginManager).logout();
        hsmLoginManager.logout();
        verify(loginManager,Mockito.times(1)).logout();
    }
}