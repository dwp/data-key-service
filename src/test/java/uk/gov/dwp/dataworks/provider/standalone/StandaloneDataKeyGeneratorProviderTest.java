package uk.gov.dwp.dataworks.provider.standalone;

import org.junit.Assert;
import org.junit.Test;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;

public class StandaloneDataKeyGeneratorProviderTest {

    private final StandaloneCurrentKeyIdProvider currentKeyIdProvider = new StandaloneCurrentKeyIdProvider();
    private final StandaloneDataKeyGeneratorProvider generatorProvider = new StandaloneDataKeyGeneratorProvider();

    private final String correlationId = "correlation";

    @Test
    public void canGenerateKeys() {
        // Create a key
        GenerateDataKeyResponse keys = generatorProvider.generateDataKey(currentKeyIdProvider.getKeyId(correlationId), correlationId);
        Assert.assertEquals(keys.dataKeyEncryptionKeyId, currentKeyIdProvider.getKeyId(correlationId));
        Assert.assertNotNull(keys.ciphertextDataKey);
        Assert.assertNotNull(keys.plaintextDataKey);
    }
}
