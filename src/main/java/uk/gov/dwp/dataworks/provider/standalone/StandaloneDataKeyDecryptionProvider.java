package uk.gov.dwp.dataworks.provider.standalone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.hsm.EncryptingCaviumCryptoImplementationSupplier;
import uk.gov.dwp.dataworks.util.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.Base64;

@Service
@Profile("STANDALONE")
public class StandaloneDataKeyDecryptionProvider implements DataKeyDecryptionProvider {
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();

    @Autowired
    public StandaloneDataKeyDecryptionProvider() {

    }

    @Override
    public DecryptDataKeyResponse decryptDataKey(String dataKeyEncryptionKeyId, String ciphertextDataKey, String correlationId) {
        LOGGER.debug("decryptDataKey: dataKeyEncryptionKeyId: '{}', ciphertextDataKey: '{}', correlation_id: {}", dataKeyEncryptionKeyId, ciphertextDataKey, correlationId);
        ByteBuffer ciphertextDataKeyBuffer = ByteBuffer.wrap(decoder.decode(ciphertextDataKey));
        byte[] decrypted = ciphertextDataKeyBuffer.array();
        // reverse the bytes
        ArrayUtils.reverse(decrypted);
        String plaintext = encoder.encodeToString(decrypted);
        DecryptDataKeyResponse response = new DecryptDataKeyResponse(dataKeyEncryptionKeyId, plaintext);
        LOGGER.debug("decryptDataKey: response.hashcode(): '{}', correlation_id: {}", response.hashCode(), correlationId);
        return response;
    }

    @Override
    public boolean canSeeDependencies() {
        return true;
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(StandaloneDataKeyDecryptionProvider.class);
}
