package uk.gov.dwp.dataworks.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import sun.security.util.ArrayUtil;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;

import java.nio.ByteBuffer;
import java.util.Base64;

@Service
@Profile("Standalone")
public class StandaloneDataKeyDecryptionProvider implements DataKeyDecryptionProvider {
    private Base64.Encoder encoder = Base64.getEncoder();
    private Base64.Decoder decoder = Base64.getDecoder();

    @Autowired
    public StandaloneDataKeyDecryptionProvider(){

    }

    @Override
    public DecryptDataKeyResponse decryptDataKey(String dataKeyEncryptionKeyId, String ciphertextDataKey) {
        ByteBuffer ciphertextDataKeyBuffer = ByteBuffer.wrap(decoder.decode(ciphertextDataKey));
        byte[] decrypted = ciphertextDataKeyBuffer.array();
        ArrayUtil.reverse(decrypted);
        String plaintext = encoder.encodeToString(decrypted);
        return new DecryptDataKeyResponse(dataKeyEncryptionKeyId,
                plaintext);
    }

    @Override
    public boolean canSeeDependencies() {
        return true;
    }
}
