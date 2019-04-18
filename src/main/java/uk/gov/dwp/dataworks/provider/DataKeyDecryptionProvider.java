package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;

public interface DataKeyDecryptionProvider {
    DecryptDataKeyResponse decryptDataKey(String dataKeyEncryptionKeyId, String ciphertextDataKey);
}
