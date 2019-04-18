package uk.gov.dwp.dataworks.provider;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.*;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionFailure;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class KMSDataKeyDecryptionProvider implements DataKeyDecryptionProvider {
    private static final int IV_SIZE = 16;
    private Base64.Encoder encoder = Base64.getEncoder();
    private Base64.Decoder decoder = Base64.getDecoder();

    public DecryptDataKeyResponse decryptDataKey(String dataKeyEncryptionKeyId, String ciphertextDataKey) {
        AWSKMS kmsClient = AWSKMSClientBuilder.defaultClient();

        ByteBuffer ciphertextDataKeyBuffer = ByteBuffer.wrap(decoder.decode(ciphertextDataKey));
        DecryptRequest req = new DecryptRequest().withCiphertextBlob(ciphertextDataKeyBuffer);

        try {
            DecryptResult result = kmsClient.decrypt(req);

            byte[] iv = new byte[IV_SIZE];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            return new DecryptDataKeyResponse(
                    result.getKeyId(),
                    encoder.encodeToString(iv),
                    encoder.encodeToString(result.getPlaintext().array())
            );
        }
        catch(NotFoundException | DisabledException | InvalidCiphertextException | KeyUnavailableException |
                DependencyTimeoutException | InvalidGrantTokenException | KMSInternalException |
                KMSInvalidStateException | NoSuchAlgorithmException ex) {
            throw new DataKeyDecryptionFailure();
        }
    }
}
