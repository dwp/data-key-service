package uk.gov.dwp.dataworks.provider.hsm;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dwp.dataworks.errors.LoginException;

import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"UnitTest", "HSM", "ExplicitHSMLogin"})
@TestPropertySource(properties = {"server.environment_name=development",
        "credentials.cache.eviction.interval=1000",
        "scheduling.enabled=false"
})
public class HSMCredentialsProviderTest {

    private static final String DEVELOPMENT_CRYPTO_USER = "development_crypto_user";
    private static final String DEVELOPMENT_CRYPTO_USER_PASSWORD = "development_crypto_user.password";
    private static final String DEVELOPMENT_CRYPTO_USER_PARTITION_ID = "hsm_partitionid";

    @Autowired
    private CacheManager cacheManager;

    @Before
    public void init() {
        Mockito.reset(awsSimpleSystemsManagement);
        for (String name : cacheManager.getCacheNames()) {
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        }
    }

    @After
    public void tearDown(){
        ReflectionTestUtils.setField(hsmCredentialsProvider, "environmentName", "development");
    }

    @Test
    public void Should_Return_Credentials_When_SSM_Has_Values() {
        String expectedPwd = DEVELOPMENT_CRYPTO_USER_PASSWORD;
        String expectedPartitionId = DEVELOPMENT_CRYPTO_USER_PARTITION_ID;
        GetParameterRequest pwdRequest = getGetParameterRequest(expectedPwd);
        GetParameterRequest partitionIdrequest = getGetParameterRequest(expectedPartitionId);
        GetParameterResult pwdResult = getGetParameterResult(expectedPwd);
        GetParameterResult partitionResult = getGetParameterResult(expectedPartitionId);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(partitionIdrequest)).willReturn(partitionResult);

        assertEquals(expectedPwd, hsmCredentialsProvider.getCredentials().getPassWord());
        assertEquals(expectedPartitionId, hsmCredentialsProvider.getCredentials().getPartitionId());
        assertEquals(DEVELOPMENT_CRYPTO_USER, hsmCredentialsProvider.getCredentials().getUserName());
    }

    @Test
    public void Should_Verify_Cache_Returns_Credentials_When_Invoked_With_In_Cache_Eviction_Interval() {
        String expectedPwd = DEVELOPMENT_CRYPTO_USER_PASSWORD;
        String expectedPartitionId = DEVELOPMENT_CRYPTO_USER_PARTITION_ID;
        GetParameterRequest pwdRequest = getGetParameterRequest(expectedPwd);
        GetParameterRequest partitionIdRequest = getGetParameterRequest(expectedPartitionId);
        GetParameterResult pwdResult = getGetParameterResult(expectedPwd);
        GetParameterResult partitionResult = getGetParameterResult(expectedPartitionId);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(partitionIdRequest)).willReturn(partitionResult);

        hsmCredentialsProvider.getCredentials();
        hsmCredentialsProvider.getCredentials();
        hsmCredentialsProvider.getCredentials();

        // Verification
        Mockito.verify(awsSimpleSystemsManagement, Mockito.times(1)).getParameter(pwdRequest);
        Mockito.verify(awsSimpleSystemsManagement, Mockito.times(1)).getParameter(partitionIdRequest);
    }

    @Test
    public void Should_Verify_Cache_Evicts_At_Specified_Interval() throws InterruptedException {
        String expectedPwd = DEVELOPMENT_CRYPTO_USER_PASSWORD;
        String expectedPartitionId = DEVELOPMENT_CRYPTO_USER_PARTITION_ID;
        GetParameterRequest pwdRequest = getGetParameterRequest(expectedPwd);
        GetParameterRequest partitionIdRequest = getGetParameterRequest(expectedPartitionId);
        GetParameterResult pwdResult = getGetParameterResult(expectedPwd);
        GetParameterResult partitionResult = getGetParameterResult(expectedPartitionId);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(partitionIdRequest)).willReturn(partitionResult);

        hsmCredentialsProvider.getCredentials();
        Thread.sleep(2000);
        hsmCredentialsProvider.getCredentials();

        // Verification
        Mockito.verify(awsSimpleSystemsManagement, Mockito.times(2)).getParameter(pwdRequest);
        Mockito.verify(awsSimpleSystemsManagement, Mockito.times(2)).getParameter(partitionIdRequest);
    }

    @Test
    public void Should_Throw_LoginException_When_Env_Property_Is_Not_Set() {
        ReflectionTestUtils.setField(hsmCredentialsProvider, "environmentName", null);

        try {
            hsmCredentialsProvider.getCredentials();
            fail("Expected a LoginException");
        } catch (LoginException ex) {
            assertEquals("Failed to retrieve the HSM credentials: LoginException - Unknown environment", ex.getMessage());
        }

        ReflectionTestUtils.setField(hsmCredentialsProvider, "environmentName", "development");
    }

    @Test
    public void Should_Throw_LoginException_When_Env_Property_Is_Empty() {
        ReflectionTestUtils.setField(hsmCredentialsProvider, "environmentName", "");

        try {
            hsmCredentialsProvider.getCredentials();
            fail("Expected a LoginException");
        } catch (LoginException ex) {
            assertEquals("Failed to retrieve the HSM credentials: LoginException - Unknown environment", ex.getMessage());
        }

        ReflectionTestUtils.setField(hsmCredentialsProvider, "environmentName", "development");
    }

    @Test
    public void Should_Throw_LoginException_When_SSM_Doesnt_Have_All_Values() {
        GetParameterRequest pwdRequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_PASSWORD);
        GetParameterRequest partitionIdRequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_PARTITION_ID);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(null);
        given(awsSimpleSystemsManagement.getParameter(partitionIdRequest)).willReturn(null);

        try {
            hsmCredentialsProvider.getCredentials();
            fail("Expected a LoginException");
        } catch (LoginException ex) {
            assertEquals("Failed to retrieve the HSM credentials: NullPointerException - null", ex.getMessage());
        }
    }

    @Test
    public void Should_Throw_LoginException_When_SSM_Doesnt_Have_Pwd() {
        String expectedClusterId = DEVELOPMENT_CRYPTO_USER_PARTITION_ID;
        GetParameterRequest pwdRequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_PASSWORD);
        GetParameterRequest partitionIdRequest = getGetParameterRequest(expectedClusterId);
        GetParameterResult partitionResult = getGetParameterResult(expectedClusterId);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(null);
        given(awsSimpleSystemsManagement.getParameter(partitionIdRequest)).willReturn(partitionResult);

        try {
            hsmCredentialsProvider.getCredentials();
            fail("Expected a LoginException");
        } catch (LoginException ex) {
            assertEquals("Failed to retrieve the HSM credentials: NullPointerException - null", ex.getMessage());
        }
    }

    @Test
    public void Should_Throw_LoginException_When_SSM_Has_Pwd_Empty() {
        String expectedPartitionId = DEVELOPMENT_CRYPTO_USER_PARTITION_ID;
        GetParameterRequest pwdRequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_PASSWORD);
        GetParameterRequest partitionIdrequest = getGetParameterRequest(expectedPartitionId);
        GetParameterResult pwdResult = getGetParameterResult("");
        GetParameterResult partitionResult = getGetParameterResult(expectedPartitionId);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(partitionIdrequest)).willReturn(partitionResult);

        try {
            hsmCredentialsProvider.getCredentials();
            fail("Expected a LoginException");
        } catch (LoginException ex) {
            assertEquals("Failed to retrieve the HSM credentials: LoginException - Either username or password is null or empty", ex.getMessage());
        }
    }

    @Test
    public void Should_Throw_LoginException_When_SSM_Has_ClusterId_Empty() {
        String expectedPwd = DEVELOPMENT_CRYPTO_USER_PASSWORD;
        GetParameterRequest pwdRequest = getGetParameterRequest(expectedPwd);
        GetParameterRequest partitionIdrequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_PARTITION_ID);
        GetParameterResult pwdResult = getGetParameterResult(expectedPwd);
        GetParameterResult partitionResult = getGetParameterResult("");
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(partitionIdrequest)).willReturn(partitionResult);

        try {
            hsmCredentialsProvider.getCredentials();
            fail("Expected a LoginException");
        } catch (LoginException ex) {
            assertEquals("Failed to retrieve the HSM credentials: LoginException - Either username or password is null or empty", ex.getMessage());
        }
    }

    @Test
    public void Should_Throw_LoginException_When_SSM_Doesnt_Have_ClusterId() {
        String expectedPwd = DEVELOPMENT_CRYPTO_USER_PASSWORD;
        GetParameterRequest pwdRequest = getGetParameterRequest(expectedPwd);
        GetParameterResult pwdResult = getGetParameterResult(expectedPwd);
        GetParameterRequest partitionIdRequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_PARTITION_ID);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(partitionIdRequest)).willReturn(null);

        try {
            hsmCredentialsProvider.getCredentials();
            fail("Expected a LoginException");
        } catch (LoginException ex) {
            assertEquals("Failed to retrieve the HSM credentials: NullPointerException - null", ex.getMessage());
        }
    }

    private GetParameterResult getGetParameterResult(String result) {
        Parameter parameter = new Parameter();
        parameter.setValue(result);
        GetParameterResult parameterResult = new GetParameterResult();
        parameterResult.setParameter(parameter);
        return parameterResult;
    }

    private GetParameterRequest getGetParameterRequest(String request) {
        return new GetParameterRequest().withName(request).withWithDecryption(true);
    }

    private GetParameterRequest getGetParameterClusterIdRequest() {
        return new GetParameterRequest().withName(DEVELOPMENT_CRYPTO_USER_PARTITION_ID).withWithDecryption(true);
    }

    @Autowired
    private HsmCredentialsProvider hsmCredentialsProvider;

    @MockBean
    private AWSSimpleSystemsManagement awsSimpleSystemsManagement;
}
