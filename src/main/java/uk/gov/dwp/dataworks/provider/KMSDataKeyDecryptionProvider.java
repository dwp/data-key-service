package uk.gov.dwp.dataworks.provider;

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

import java.nio.ByteBuffer;
import java.util.Base64;

@Service
@Profile("KMS")
public class KMSDataKeyDecryptionProvider implements DataKeyDecryptionProvider {

    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final AWSKMS awsKms;

    @Autowired
    public KMSDataKeyDecryptionProvider(AWSKMS awsKms) {
        this.awsKms = awsKms;
    }

    public DecryptDataKeyResponse decryptDataKey(String keyId, String ciphertextDataKey) {
        try {

            if (Strings.isNullOrEmpty(keyId) || Strings.isNullOrEmpty(ciphertextDataKey) ||
                    ciphertextDataKey.getBytes().length > DataKeyDecryptionProvider.MAX_PAYLOAD_SIZE) {
                throw new UnusableParameterException();
            }

            ByteBuffer ciphertextDataKeyBuffer = ByteBuffer.wrap(decoder.decode(ciphertextDataKey));
            DecryptRequest request = new DecryptRequest().withCiphertextBlob(ciphertextDataKeyBuffer);
            DecryptResult result = this.awsKms.decrypt(request);
            return new DecryptDataKeyResponse(result.getKeyId(),
                    encoder.encodeToString(result.getPlaintext().array()));
        }
        catch (UnusableParameterException e) {
            throw e;
        }
        catch (IllegalArgumentException | InvalidCiphertextException ex) {
            throw new GarbledDataKeyException(ex);
        }
        catch (RuntimeException ex) {
            throw new DataKeyDecryptionException(ex);
        }
    }
}
