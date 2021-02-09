package uk.gov.dwp.dataworks.provider.hsm;

import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.HsmLoginManager;

@Service
@Profile("HSM")
public class HsmDataKeyDecryptionProvider extends HsmDependent
        implements DataKeyDecryptionProvider, HsmDataKeyDecryptionConstants {

    HsmDataKeyDecryptionProvider(HsmLoginManager loginManager,
                                 CryptoImplementationSupplier cryptoImplementationSupplier) {
        super(loginManager);
        this.cryptoImplementationSupplier = cryptoImplementationSupplier;
    }

    @Override
    @Retryable(value = {MasterKeystoreException.class},
            maxAttemptsExpression = "${hsm.retry.maxAttempts:5}",
            backoff = @Backoff(delayExpression = "${hsm.retry.delay:1000}",
                               multiplierExpression = "${hsm.retry.multiplier:2.0}"))
    public DecryptDataKeyResponse decryptDataKey(String decryptionKeyId, String ciphertextDataKey, String correlationId)
            throws MasterKeystoreException {
        try {
            loginManager.login();
            Integer decryptionKeyHandle = privateKeyHandle(decryptionKeyId, correlationId);
            String decryptedKey = cryptoImplementationSupplier.decryptedKey(decryptionKeyHandle, ciphertextDataKey, correlationId);
            return new DecryptDataKeyResponse(decryptionKeyId, decryptedKey);
        } catch (CryptoImplementationSupplierException e) {
            throw new DataKeyDecryptionException(correlationId);
        }
    }

    @Override
    public boolean canSeeDependencies() {
        return this.loginManager != null && this.cryptoImplementationSupplier != null;
    }

    private final CryptoImplementationSupplier cryptoImplementationSupplier;
}
