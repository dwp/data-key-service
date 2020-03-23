package uk.gov.dwp.dataworks.provider.kms;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;
import uk.gov.dwp.dataworks.logging.DataworksLogger;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

import java.util.Base64;

@Service
@Profile("KMS")
public class KMSDataKeyGeneratorProvider implements DataKeyGeneratorProvider {
    private final AWSKMS awsKms;
    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(KMSDataKeyGeneratorProvider.class.toString());

    @Autowired
    public KMSDataKeyGeneratorProvider(AWSKMS awsKms) {
        this.awsKms = awsKms;
    }

    @Override
    public GenerateDataKeyResponse generateDataKey(String encryptionKeyId, String correlationId) throws DataKeyGenerationException {
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
            dataKeyRequest.setKeyId(encryptionKeyId);
            dataKeyRequest.setKeySpec("AES_128");
            GenerateDataKeyResult result = awsKms.generateDataKey(dataKeyRequest);
            return new GenerateDataKeyResponse(encryptionKeyId, encoder.encodeToString(result.getPlaintext().array()),
                    encoder.encodeToString(result.getCiphertextBlob().array()));
        }
        catch (NotFoundException | DisabledException | KeyUnavailableException | DependencyTimeoutException | InvalidKeyUsageException | InvalidGrantTokenException | KMSInternalException | KMSInvalidStateException ex) {
            DataKeyGenerationException wrapper = new DataKeyGenerationException(correlationId);
            LOGGER.error(wrapper.getMessage(), ex);
            throw wrapper;
        }
    }

    @Override
    public boolean canSeeDependencies() {
        return awsKms != null;
    }
}
