package uk.gov.dwp.dataworks.provider.aws;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;

import static org.mockito.BDDMockito.given;

//@RunWith(SpringRunner.class)
//@SpringBootTest()
//@ActiveProfiles({"KMS", "UnitTest"})
public class AWSCurrentKeyIdProviderTest {

    @Before
    public void init() {
        Mockito.reset(awsSimpleSystemsManagement);
    }

//    @Test
//    public void getKeyIdReturnsCurrentKeyId() {
//        String expectedKeyId = "currentKeyId";
//        GetParameterRequest request = getGetParameterRequest();
//        GetParameterResult result = getGetParameterResult(expectedKeyId);
//        given(awsSimpleSystemsManagement.getParameter(request)).willReturn(result);
//        System.err.println("currentKeyIdProvider: '" + currentKeyIdProvider + "'");
//        Assert.assertEquals(expectedKeyId, currentKeyIdProvider.getKeyId());
//    }

//    @Test(expected = CurrentKeyIdException.class)
//    public void handlesInternalServerErrorException() {
//        throwException(InternalServerErrorException.class);
//    }
//
//    @Test(expected = CurrentKeyIdException.class)
//    public void handlesInvalidKeyIdException() {
//        throwException(InvalidKeyIdException.class);
//    }
//
//    @Test(expected = CurrentKeyIdException.class)
//    public void handlesParameterNotFoundException() {
//        throwException(ParameterNotFoundException.class);
//    }
//
//    @Test(expected = CurrentKeyIdException.class)
//    public void handlesRuntimeException() {
//        throwException(RuntimeException.class);
//    }

    private void throwException(Class<? extends Exception> e) throws CurrentKeyIdException {
        given(awsSimpleSystemsManagement.getParameter(ArgumentMatchers.any(GetParameterRequest.class))).willThrow(e);
        System.err.println("currentKeyIdProvider: '" + currentKeyIdProvider + "'");
        currentKeyIdProvider.getKeyId();
    }

    private GetParameterResult getGetParameterResult(String expectedKeyId) {
        Parameter parameter = new Parameter();
        parameter.setValue(expectedKeyId);
        GetParameterResult result = new GetParameterResult();
        result.setParameter(parameter);
        return result;
    }

    private GetParameterRequest getGetParameterRequest() {
        return new GetParameterRequest().withName("data_key_service.currentKeyId").withWithDecryption(false);
    }

    @Autowired
    private CurrentKeyIdProvider currentKeyIdProvider;

    @Autowired
    private AWSSimpleSystemsManagement awsSimpleSystemsManagement;
}