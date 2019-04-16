package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import org.junit.Test;
import org.springframework.util.Assert;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationFailure;

public class KMSDataKeyGeneratorProviderTest {

    //@Test
    public void canGenerateDataKey() {
        KMSCurrentKeyIdProvider keyIdProvider = new KMSCurrentKeyIdProvider();
        KMSDataKeyGeneratorProvider providerUnderTest = new KMSDataKeyGeneratorProvider();

        GenerateDataKeyResponse result = providerUnderTest.generateDataKey(keyIdProvider.getKeyId());
        Assert.notNull(result, "Must return a result");
    }


    //@Test(expected = DataKeyGenerationFailure.class)
    public void canGenerateDataKeyFailureWithException() {
        KMSDataKeyGeneratorProvider providerUnderTest = new KMSDataKeyGeneratorProvider();
        providerUnderTest.generateDataKey("frederick no keys");
    }


    /* Missing tests:
        - Service failure
        - keyId invalid/missing
        - disabled key

     */
}
