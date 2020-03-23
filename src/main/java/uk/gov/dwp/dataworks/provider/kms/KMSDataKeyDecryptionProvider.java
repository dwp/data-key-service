package uk.gov.dwp.dataworks.provider.kms;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.InvalidCiphertextException;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionException;
import uk.gov.dwp.dataworks.errors.GarbledDataKeyException;
import uk.gov.dwp.dataworks.errors.UnusableParameterException;
import uk.gov.dwp.dataworks.logging.DataworksLogger;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;

import java.nio.ByteBuffer;
import java.util.Base64;

@Service
@Profile("KMS")
public class KMSDataKeyDecryptionProvider implements DataKeyDecryptionProvider {

    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final AWSKMS awsKms;
    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(KMSDataKeyDecryptionProvider.class.toString());

    @Autowired
    public KMSDataKeyDecryptionProvider(AWSKMS awsKms) {
        this.awsKms = awsKms;
    }

    public DecryptDataKeyResponse decryptDataKey(String keyId, String ciphertextDataKey, String correlationId) {
        try {
            if (Strings.isNullOrEmpty(keyId) || Strings.isNullOrEmpty(ciphertextDataKey) ||
                    ciphertextDataKey.getBytes().length > DataKeyDecryptionProvider.MAX_PAYLOAD_SIZE) {
                throw new UnusableParameterException(correlationId);
            }

            ByteBuffer ciphertextDataKeyBuffer = ByteBuffer.wrap(decoder.decode(ciphertextDataKey));
            DecryptRequest request = new DecryptRequest().withCiphertextBlob(ciphertextDataKeyBuffer);
            DecryptResult result = this.awsKms.decrypt(request);
            return new DecryptDataKeyResponse(result.getKeyId(),
                    encoder.encodeToString(result.getPlaintext().array()));
        } catch (UnusableParameterException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        } catch (IllegalArgumentException | InvalidCiphertextException ex) {
            GarbledDataKeyException wrapper = new GarbledDataKeyException(correlationId);
            LOGGER.error(wrapper.getMessage(), ex);
            throw wrapper;
        } catch (RuntimeException ex) {
            DataKeyDecryptionException wrapper = new DataKeyDecryptionException(correlationId);
            LOGGER.error(wrapper.getMessage(), ex);
            throw wrapper;
        }
    }

    @Override
    public boolean canSeeDependencies() {
        return awsKms != null;
    }
}
