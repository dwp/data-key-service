package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;

public interface DataKeyDecryptionProvider extends Dependent {
    int MAX_PAYLOAD_SIZE = 32000;
    DecryptDataKeyResponse decryptDataKey(String keyId, String ciphertextDataKey);
}
