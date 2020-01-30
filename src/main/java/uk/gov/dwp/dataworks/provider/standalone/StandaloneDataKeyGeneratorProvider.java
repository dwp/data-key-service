package uk.gov.dwp.dataworks.provider.standalone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.util.ArrayUtils;

import java.util.Base64;
import java.util.Random;

@Service
@Profile("STANDALONE")
public class StandaloneDataKeyGeneratorProvider implements DataKeyGeneratorProvider {
    private final Base64.Encoder encoder = Base64.getEncoder();

    @Autowired
    public StandaloneDataKeyGeneratorProvider() {

    }

    @Override
    public GenerateDataKeyResponse generateDataKey(String keyId, String correlationId) {
        LOGGER.debug("generateDataKey: keyId: '{}', correlation_id: {}", keyId, correlationId);
        // Generate a random key
        int keySize = 128 / 8;
        byte[] key = new byte[keySize];
        new Random().nextBytes(key);
        String plaintextKey = encoder.encodeToString(key);
        // reverse the bytes
        ArrayUtils.reverse(key);
        String encryptedKey = encoder.encodeToString(key);
        GenerateDataKeyResponse response = new GenerateDataKeyResponse(keyId, plaintextKey, encryptedKey);
        LOGGER.debug("generateDataKey: response.hashCode(): '{}', correlation_id: {}", response.hashCode(), correlationId);
        return response;
    }

    @Override
    public boolean canSeeDependencies() {
        return true;
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(StandaloneDataKeyGeneratorProvider.class);
}
