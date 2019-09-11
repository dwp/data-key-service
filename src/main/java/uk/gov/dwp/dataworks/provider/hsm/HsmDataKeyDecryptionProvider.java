package uk.gov.dwp.dataworks.provider.hsm;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionException;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;

@Service
@Profile("HSM")
public class HsmDataKeyDecryptionProvider extends HsmDependent
        implements DataKeyDecryptionProvider, HsmDataKeyDecryptionConstants {

    HsmDataKeyDecryptionProvider(CryptoImplementationSupplier cryptoImplementationSupplier) {
        this.cryptoImplementationSupplier = cryptoImplementationSupplier;
    }

    @Override
    public DecryptDataKeyResponse decryptDataKey(String decryptionKeyId, String ciphertextDataKey) {
        try {
            loginManager.login();
            Integer decryptionKeyHandle = privateKeyHandle(decryptionKeyId);
            String decryptedKey = cryptoImplementationSupplier.decryptedKey(decryptionKeyHandle, ciphertextDataKey);
            loginManager.logout();
            return new DecryptDataKeyResponse(decryptionKeyId, decryptedKey);
        }
        catch (CryptoImplementationSupplierException e) {
            throw new DataKeyDecryptionException();
        }
    }
}
