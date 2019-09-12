package uk.gov.dwp.dataworks.provider.hsm;

import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;

import java.security.Key;

public interface CryptoImplementationSupplier {
    Key dataKey() throws CryptoImplementationSupplierException;
    byte[] encryptedKey(Integer wrappingKeyHandle, Key dataKey) throws CryptoImplementationSupplierException;
    String decryptedKey(Integer decryptionKeyHandle, String ciphertextDataKey) throws CryptoImplementationSupplierException;
}
