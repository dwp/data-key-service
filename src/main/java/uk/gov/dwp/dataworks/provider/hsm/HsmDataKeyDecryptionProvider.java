package uk.gov.dwp.dataworks.provider.hsm;

import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.provider.HsmLoginManager;

@Service
@Profile("HSM")
public class HsmDataKeyDecryptionProvider extends HsmDependent
        implements DataKeyDecryptionProvider, HsmDataKeyDecryptionConstants {

    private final int MAX_ATTEMPTS = 10;

    HsmDataKeyDecryptionProvider(CurrentKeyIdProvider currentKeyIdProvider,
                                 DataKeyGeneratorProvider dataKeyGeneratorProvider,
                                 HsmLoginManager loginManager,
                                 CryptoImplementationSupplier cryptoImplementationSupplier) {
        super(loginManager);
        this.cryptoImplementationSupplier = cryptoImplementationSupplier;
    }

    @Override
    @Retryable(
            value = {MasterKeystoreException.class},
            maxAttempts = MAX_ATTEMPTS,
            backoff = @Backoff(delay = INITIAL_BACKOFF_MILLIS, multiplier = BACKOFF_MULTIPLIER))
    public DecryptDataKeyResponse decryptDataKey(String decryptionKeyId, String ciphertextDataKey, String correlationId)
            throws MasterKeystoreException {
        try {
            loginManager.login();
            Integer decryptionKeyHandle = privateKeyHandle(decryptionKeyId, correlationId);
            String decryptedKey = cryptoImplementationSupplier.decryptedKey(decryptionKeyHandle, ciphertextDataKey, correlationId);
            return new DecryptDataKeyResponse(decryptionKeyId, decryptedKey);
        } catch (CryptoImplementationSupplierException e) {
            throw new DataKeyDecryptionException(correlationId);
        } finally {
            loginManager.logout();
        }
    }

    @Override
    public boolean canSeeDependencies() {
        return this.loginManager != null && this.cryptoImplementationSupplier != null;
    }

    private final CryptoImplementationSupplier cryptoImplementationSupplier;
}
