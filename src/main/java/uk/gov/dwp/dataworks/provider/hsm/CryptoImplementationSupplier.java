package uk.gov.dwp.dataworks.provider.hsm;

import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;

import java.security.Key;

public interface CryptoImplementationSupplier {
    Key dataKey() throws CryptoImplementationSupplierException, MasterKeystoreException;
    byte[] encryptedKey(Integer wrappingKeyHandle, Key dataKey)
            throws CryptoImplementationSupplierException, MasterKeystoreException;
    String decryptedKey(Integer decryptionKeyHandle, String ciphertextDataKey)
            throws CryptoImplementationSupplierException, MasterKeystoreException;
}
