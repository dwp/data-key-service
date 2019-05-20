package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import org.springframework.util.Assert;

public class KMSDataKeyGeneratorProviderTest {

    //@Test
    public void canGenerateDataKey() {
        KMSCurrentKeyIdProvider keyIdProvider = new KMSCurrentKeyIdProvider();
        KMSDataKeyGeneratorProvider providerUnderTest = new KMSDataKeyGeneratorProvider();

        GenerateDataKeyResponse result = providerUnderTest.generateDataKey(keyIdProvider.getKeyId());
        Assert.notNull(result, "Must return a result");
    }


    //@Test(expected = DataKeyGenerationException.class)
    public void canGenerateDataKeyFailureWithException() {
        KMSDataKeyGeneratorProvider providerUnderTest = new KMSDataKeyGeneratorProvider();
        providerUnderTest.generateDataKey("frederick no keys");
    }

}
