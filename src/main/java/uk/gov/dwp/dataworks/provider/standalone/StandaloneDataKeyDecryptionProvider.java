package uk.gov.dwp.dataworks.provider.standalone;

import kotlin.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.logging.DataworksLogger;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
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
        LOGGER.debug("Decrypting datakey",
                new Pair<>("datakey_encryption_key_id", dataKeyEncryptionKeyId),
                new Pair<>("ciphertext_datakey", ciphertextDataKey),
                new Pair<>("correlation_id", correlationId));
        ByteBuffer ciphertextDataKeyBuffer = ByteBuffer.wrap(decoder.decode(ciphertextDataKey));
        byte[] decrypted = ciphertextDataKeyBuffer.array();
        // reverse the bytes
        ArrayUtils.reverse(decrypted);
        String plaintext = encoder.encodeToString(decrypted);
        DecryptDataKeyResponse response = new DecryptDataKeyResponse(dataKeyEncryptionKeyId, plaintext);
        LOGGER.debug("Decrypting datakey",
                new Pair<>("response_hashcode", response.hashCode() + ""),
                new Pair<>("correlation_id", correlationId));
        return response;
    }

    @Override
    public boolean canSeeDependencies() {
        return true;
    }

    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(StandaloneDataKeyDecryptionProvider.class.toString());
}
