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
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dwp.dataworks.dto.HSMCredentials;
import uk.gov.dwp.dataworks.provider.HsmLoginManager;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ImplicitHsmLoginManager.class)
@ActiveProfiles({"ImplicitHSMLogin"})
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
    public void Should_Not_Explicitly_Login_When_Credentials_Are_Not_Null() throws CFM2Exception {
        HSMCredentials hsmCredentials = new HSMCredentials(CRYPTO_USER, CRYPTO_USER_PASSWORD, CRYPTO_USER_PARTITION_ID);
        given(hsmCredentialsProvider.getCredentials()).willReturn(hsmCredentials);
        doNothing().when(loginManager).login(CRYPTO_USER_PARTITION_ID, CRYPTO_USER, CRYPTO_USER_PASSWORD);
        hsmLoginManager.login();
        verifyNoMoreInteractions(loginManager);

    }
}
