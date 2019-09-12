package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.LoginException;

public interface DataKeyDecryptionProvider extends Dependent {
    int MAX_PAYLOAD_SIZE = 32000;
    DecryptDataKeyResponse decryptDataKey(String keyId, String ciphertextDataKey);
}
