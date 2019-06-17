package uk.gov.dwp.dataworks.provider;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;

import java.util.Base64;

@Service
@Profile("KMS")
public class KMSDataKeyGeneratorProvider implements DataKeyGeneratorProvider {
    private final AWSKMS awsKms;

    @Autowired
    public KMSDataKeyGeneratorProvider(AWSKMS awsKms) {
        this.awsKms = awsKms;
    }

    public GenerateDataKeyResponse generateDataKey(String keyId) throws DataKeyGenerationException {
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
            dataKeyRequest.setKeyId(keyId);
            dataKeyRequest.setKeySpec("AES_128");
            GenerateDataKeyResult result = awsKms.generateDataKey(dataKeyRequest);
            return new GenerateDataKeyResponse(result.getKeyId(), encoder.encodeToString(result.getPlaintext().array()),
                    encoder.encodeToString(result.getCiphertextBlob().array()));
        }
        catch (NotFoundException | DisabledException | KeyUnavailableException | DependencyTimeoutException | InvalidKeyUsageException | InvalidGrantTokenException | KMSInternalException | KMSInvalidStateException ex) {
            throw new DataKeyGenerationException(ex);
        }
    }
}
