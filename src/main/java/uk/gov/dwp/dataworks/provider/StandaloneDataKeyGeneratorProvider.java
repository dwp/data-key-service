package uk.gov.dwp.dataworks.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import sun.security.util.ArrayUtil;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;

import java.util.Base64;
import java.util.Random;

@Service
@Profile("Standalone")
public class StandaloneDataKeyGeneratorProvider implements DataKeyGeneratorProvider {
    private Base64.Encoder encoder = Base64.getEncoder();

    @Autowired
    public StandaloneDataKeyGeneratorProvider(){

    }

    public GenerateDataKeyResponse generateDataKey(String keyId) {

        // Generate a random key
        int keySize = 128/8;
        byte[] key = new byte[keySize];
        new Random().nextBytes(key);
        String plaintextKey = encoder.encodeToString(key);

        // reverse the bytes
        ArrayUtil.reverse(key);
        String encryptedKey = encoder.encodeToString(key);

        return new GenerateDataKeyResponse(
                keyId,
                plaintextKey,
                encryptedKey
        );

    }

    @Override
    public boolean canSeeDependencies() {
        return true;
    }
}
