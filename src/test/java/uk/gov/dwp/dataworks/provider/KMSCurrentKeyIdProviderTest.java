package uk.gov.dwp.dataworks.provider;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.*;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles({"UnitTest", "KMS"})
public class KMSCurrentKeyIdProviderTest {

    @Before
    public void init() {
        Mockito.reset(awsSimpleSystemsManagement);
    }

    @Test
    public void getKeyIdReturnsCurrentKeyId() {
        String expectedKeyId = "currentKeyId";
        GetParameterRequest request = getGetParameterRequest();
        GetParameterResult result = getGetParameterResult(expectedKeyId);
        given(awsSimpleSystemsManagement.getParameter(request)).willReturn(result);
        assertEquals(expectedKeyId, currentKeyIdProvider.getKeyId());
    }

    @Test
    public void handlesInternalServerErrorException() {
        try {
            throwException(InternalServerErrorException.class);
        }
        catch (CurrentKeyIdException ex) {
            assertEquals("Failed to retrieve the current key id.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + CurrentKeyIdException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesInvalidKeyIdException() {
        try {
            throwException(InvalidKeyIdException.class);
        }
        catch (CurrentKeyIdException ex) {
            assertEquals("Failed to retrieve the current key id.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + CurrentKeyIdException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesParameterNotFoundException() {
        try {
            throwException(ParameterNotFoundException.class);
        }
        catch (CurrentKeyIdException ex) {
            assertEquals("Failed to retrieve the current key id.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + CurrentKeyIdException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesRuntimeException() {
        try {
            throwException(RuntimeException.class);
        }
        catch (CurrentKeyIdException ex) {
            assertEquals("Failed to retrieve the current key id.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + CurrentKeyIdException.class + " got " + e.getClass() + ".");
        }
    }

    private void throwException(Class<? extends Exception> e) throws CurrentKeyIdException {
        given(awsSimpleSystemsManagement.getParameter(ArgumentMatchers.any(GetParameterRequest.class))).willThrow(e);
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
        return new GetParameterRequest().withName("data-key-service.currentKeyId").withWithDecryption(false);
    }

    @Autowired
    private CurrentKeyIdProvider currentKeyIdProvider;

    @Autowired
    private AWSSimpleSystemsManagement awsSimpleSystemsManagement;
}