package uk.gov.dwp.dataworks.provider.standalone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
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
    public StandaloneDataKeyDecryptionProvider(){

    }

    @Override
    public DecryptDataKeyResponse decryptDataKey(String dataKeyEncryptionKeyId, String ciphertextDataKey) {
        ByteBuffer ciphertextDataKeyBuffer = ByteBuffer.wrap(decoder.decode(ciphertextDataKey));
        byte[] decrypted = ciphertextDataKeyBuffer.array();
        ArrayUtils.reverse(decrypted);
        String plaintext = encoder.encodeToString(decrypted);
        return new DecryptDataKeyResponse(dataKeyEncryptionKeyId,
                plaintext);
    }

    @Override
    public boolean canSeeDependencies() {
        return true;
    }
}
