package uk.gov.dwp.dataworks.provider;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.*;
import org.springframework.context.annotation.Profile;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@Profile("KMS")
public class KMSDataKeyGeneratorProvider implements DataKeyGeneratorProvider {
    private Logger logger = LoggerFactory.getLogger(KMSDataKeyGeneratorProvider.class);

    public GenerateDataKeyResponse generateDataKey(String keyId) throws DataKeyGenerationException {
        Base64.Encoder encoder = Base64.getEncoder();
        AWSKMS kmsClient = AWSKMSClientBuilder.defaultClient();
        GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
        dataKeyRequest.setKeyId(keyId);
        dataKeyRequest.setKeySpec("AES_128");

        try {
            GenerateDataKeyResult result = kmsClient.generateDataKey(dataKeyRequest);
            return new GenerateDataKeyResponse(
                    result.getKeyId(),
                    encoder.encodeToString(result.getPlaintext().array()),
                    encoder.encodeToString(result.getCiphertextBlob().array())
            );
        } catch (NotFoundException | DisabledException | KeyUnavailableException | DependencyTimeoutException |
                InvalidKeyUsageException | InvalidGrantTokenException | KMSInternalException | KMSInvalidStateException ex) {
            logger.error("Exception caught while communicating with KMS", ex);
            throw new DataKeyGenerationException();
        }
    }
}
