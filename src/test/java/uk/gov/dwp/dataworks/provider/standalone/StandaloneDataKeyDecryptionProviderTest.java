package uk.gov.dwp.dataworks.provider.standalone;

import org.junit.Assert;
import org.junit.Test;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;

public class StandaloneDataKeyDecryptionProviderTest {
    private final StandaloneCurrentKeyIdProvider currentKeyIdProvider = new StandaloneCurrentKeyIdProvider();
    private final StandaloneDataKeyGeneratorProvider generatorProvider = new StandaloneDataKeyGeneratorProvider();
    private final StandaloneDataKeyDecryptionProvider decryptionProvider = new StandaloneDataKeyDecryptionProvider();

    @Test
    public void canDecryptKeys(){
        // Create key
        GenerateDataKeyResponse keys = generatorProvider.generateDataKey(currentKeyIdProvider.getKeyId());

        // Decrypt key
        DecryptDataKeyResponse decrypted = decryptionProvider.decryptDataKey(
                keys.dataKeyEncryptionKeyId,
                keys.ciphertextDataKey);

        // What went in, must come out
        Assert.assertEquals(keys.plaintextDataKey, decrypted.plaintextDataKey);
    }
}
