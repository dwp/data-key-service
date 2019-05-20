package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionException;
import uk.gov.dwp.dataworks.errors.GarbledDataKeyException;

public interface DataKeyDecryptionProvider {
    DecryptDataKeyResponse decryptDataKey(String dataKeyEncryptionKeyId, String ciphertextDataKey) throws GarbledDataKeyException, DataKeyDecryptionException;
}
