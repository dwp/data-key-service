package uk.gov.dwp.dataworks.provider.hsm;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"UnitTest", "HSM"})
@TestPropertySource(properties = {"server.environment_name=development",
        "credentials.cache.eviction.interval=1000"
})
public class HSMCredentialsProviderTest {

    private static final String DEVELOPMENT_CRYPTO_USER = "development_crypto_user";
    private static final String DEVELOPMENT_CRYPTO_USER_PASSWORD = "development_crypto_user.password";
    private static final String DEVELOPMENT_CRYPTO_USER_CLUSTER = "cluster_id";

    @Autowired
    private CacheManager cacheManager;

    @Before
    public void init() {
        Mockito.reset(awsSimpleSystemsManagement);
        //hsmCredentialsProvider.clearCache();
        for (String name : cacheManager.getCacheNames()) {
            cacheManager.getCache(name).clear();
        }
    }

    @Test
    public void Should_Return_Credentials_When_SSM_Has_Values() {
        String expectedPwd = DEVELOPMENT_CRYPTO_USER_PASSWORD;
        String expectedClusterId = DEVELOPMENT_CRYPTO_USER_CLUSTER;
        GetParameterRequest pwdRequest = getGetParameterRequest(expectedPwd);
        GetParameterRequest clusterIdrequest = getGetParameterRequest(expectedClusterId);
        GetParameterResult pwdResult = getGetParameterResult(expectedPwd);
        GetParameterResult clusterResult = getGetParameterResult(expectedClusterId);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(clusterIdrequest)).willReturn(clusterResult);
        assertEquals(expectedPwd, hsmCredentialsProvider.getCredentials().getPassWord());
        assertEquals(expectedClusterId, hsmCredentialsProvider.getCredentials().getClusterId());
        assertEquals(DEVELOPMENT_CRYPTO_USER, hsmCredentialsProvider.getCredentials().getUserName());
    }

    @Test
    public void Should_Verify_Cache_Returns_Credentials_When_Invoked_With_In_Cache_Eviction_Interval() {
        String expectedPwd = DEVELOPMENT_CRYPTO_USER_PASSWORD;
        String expectedClusterId = DEVELOPMENT_CRYPTO_USER_CLUSTER;
        GetParameterRequest pwdRequest = getGetParameterRequest(expectedPwd);
        GetParameterRequest clusterIdrequest = getGetParameterRequest(expectedClusterId);
        GetParameterResult pwdResult = getGetParameterResult(expectedPwd);
        GetParameterResult clusterResult = getGetParameterResult(expectedClusterId);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(clusterIdrequest)).willReturn(clusterResult);
        hsmCredentialsProvider.getCredentials();
        hsmCredentialsProvider.getCredentials();
        hsmCredentialsProvider.getCredentials();
        // Verification
        Mockito.verify(awsSimpleSystemsManagement, Mockito.times(1)).getParameter(pwdRequest);
        Mockito.verify(awsSimpleSystemsManagement, Mockito.times(1)).getParameter(clusterIdrequest);
    }

    @Test
    public void Should_Verify_Cache_Evicts_At_Specified_Interval() throws InterruptedException {
        String expectedPwd = DEVELOPMENT_CRYPTO_USER_PASSWORD;
        String expectedClusterId = DEVELOPMENT_CRYPTO_USER_CLUSTER;
        GetParameterRequest pwdRequest = getGetParameterRequest(expectedPwd);
        GetParameterRequest clusterIdrequest = getGetParameterRequest(expectedClusterId);
        GetParameterResult pwdResult = getGetParameterResult(expectedPwd);
        GetParameterResult clusterResult = getGetParameterResult(expectedClusterId);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(clusterIdrequest)).willReturn(clusterResult);
        hsmCredentialsProvider.getCredentials();
        Thread.sleep(2000);
        hsmCredentialsProvider.getCredentials();
        // Verification
        Mockito.verify(awsSimpleSystemsManagement, Mockito.times(2)).getParameter(pwdRequest);
        Mockito.verify(awsSimpleSystemsManagement, Mockito.times(2)).getParameter(clusterIdrequest);
    }

    @Test(expected = LoginException.class)
    public void Should_Throw_LoginException_When_Env_Property_Is_Not_Set() {
        ReflectionTestUtils.setField(hsmCredentialsProvider, "environmentName", null);
        hsmCredentialsProvider.getCredentials();
        ReflectionTestUtils.setField(hsmCredentialsProvider, "environmentName", "development");
    }

    @Test(expected = LoginException.class)
    public void Should_Throw_LoginException_When_Env_Property_Is_Empty() {
        ReflectionTestUtils.setField(hsmCredentialsProvider, "environmentName", "");
        hsmCredentialsProvider.getCredentials();
        ReflectionTestUtils.setField(hsmCredentialsProvider, "environmentName", "development");
    }

    @Test(expected = LoginException.class)
    public void Should_Throw_LoginException_When_SSM_Doesnt_Have_All_Values() {
        GetParameterRequest pwdRequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_PASSWORD);
        GetParameterRequest clusterIdrequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_CLUSTER);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(null);
        given(awsSimpleSystemsManagement.getParameter(clusterIdrequest)).willReturn(null);
        hsmCredentialsProvider.getCredentials();
    }

    @Test(expected = LoginException.class)
    public void Should_Throw_LoginException_When_SSM_Doesnt_Have_Pwd() {
        String expectedClusterId = DEVELOPMENT_CRYPTO_USER_CLUSTER;
        GetParameterRequest pwdRequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_PASSWORD);
        GetParameterRequest clusterIdrequest = getGetParameterRequest(expectedClusterId);
        GetParameterResult clusterResult = getGetParameterResult(expectedClusterId);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(null);
        given(awsSimpleSystemsManagement.getParameter(clusterIdrequest)).willReturn(clusterResult);
        hsmCredentialsProvider.getCredentials();
    }

    @Test(expected = LoginException.class)
    public void Should_Throw_LoginException_When_SSM_Has_Pwd_Empty() {
        String expectedClusterId = DEVELOPMENT_CRYPTO_USER_CLUSTER;
        GetParameterRequest pwdRequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_PASSWORD);
        GetParameterRequest clusterIdrequest = getGetParameterRequest(expectedClusterId);
        GetParameterResult pwdResult = getGetParameterResult("");
        GetParameterResult clusterResult = getGetParameterResult(expectedClusterId);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(clusterIdrequest)).willReturn(clusterResult);
        hsmCredentialsProvider.getCredentials();
    }

    @Test(expected = LoginException.class)
    public void Should_Throw_LoginException_When_SSM_Has_ClusterId_Empty() {
        String expectedPwd = DEVELOPMENT_CRYPTO_USER_PASSWORD;
        GetParameterRequest pwdRequest = getGetParameterRequest(expectedPwd);
        GetParameterRequest clusterIdrequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_CLUSTER);
        GetParameterResult pwdResult = getGetParameterResult(expectedPwd);
        GetParameterResult clusterResult = getGetParameterResult("");
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(clusterIdrequest)).willReturn(clusterResult);
        hsmCredentialsProvider.getCredentials();
    }

    @Test(expected = LoginException.class)
    public void Should_Throw_LoginException_When_SSM_Doesnt_Have_ClusterId() {
        String expectedPwd = DEVELOPMENT_CRYPTO_USER_PASSWORD;
        GetParameterRequest pwdRequest = getGetParameterRequest(expectedPwd);
        GetParameterResult pwdResult = getGetParameterResult(expectedPwd);
        GetParameterRequest clusterIdrequest = getGetParameterRequest(DEVELOPMENT_CRYPTO_USER_CLUSTER);
        given(awsSimpleSystemsManagement.getParameter(pwdRequest)).willReturn(pwdResult);
        given(awsSimpleSystemsManagement.getParameter(clusterIdrequest)).willReturn(null);
        hsmCredentialsProvider.getCredentials();
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
        return new GetParameterRequest().withName(DEVELOPMENT_CRYPTO_USER_CLUSTER).withWithDecryption(true);
    }

    @Autowired
    private HsmCredentialsProvider hsmCredentialsProvider;

    @MockBean
    private AWSSimpleSystemsManagement awsSimpleSystemsManagement;
}