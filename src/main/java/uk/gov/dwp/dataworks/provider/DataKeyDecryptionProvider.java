package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionFailure;
import uk.gov.dwp.dataworks.errors.GarbledDataKeyError;

public interface DataKeyDecryptionProvider {
    DecryptDataKeyResponse decryptDataKey(String dataKeyEncryptionKeyId, String ciphertextDataKey) throws GarbledDataKeyError, DataKeyDecryptionFailure;
}
