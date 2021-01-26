package uk.gov.dwp.dataworks.provider.hsm;

import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.logging.DataworksLogger;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.provider.HsmLoginManager;

import java.security.Key;
import java.util.Base64;

@Service
@Profile("HSM")
public class HsmDataKeyGeneratorProvider extends HsmDependent implements DataKeyGeneratorProvider {

    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(HsmDataKeyGeneratorProvider.class.toString());

    public HsmDataKeyGeneratorProvider(HsmLoginManager loginManager,
                                       CryptoImplementationSupplier cryptoImplementationSupplier) {
        super(loginManager);
        this.cryptoImplementationSupplier = cryptoImplementationSupplier;
    }

    @Override
    @Retryable(
            value = {MasterKeystoreException.class},
            maxAttempts = MAX_ATTEMPTS,
            backoff = @Backoff(delay = INITIAL_BACKOFF_MILLIS, multiplier = BACKOFF_MULTIPLIER))
    public GenerateDataKeyResponse generateDataKey(String keyId, String correlationId)
            throws DataKeyGenerationException, MasterKeystoreException {
        try {
            loginManager.login();
            int publicKeyHandle = publicKeyHandle(keyId, correlationId);
            Key dataKey = cryptoImplementationSupplier.dataKey(correlationId);
            byte[] plaintextDatakey = Base64.getEncoder().encode(dataKey.getEncoded());
            byte[] ciphertext = cryptoImplementationSupplier.encryptedKey(publicKeyHandle, dataKey, correlationId);
            return new GenerateDataKeyResponse(keyId,
                    new String(plaintextDatakey),
                    new String(ciphertext));
        } catch (CryptoImplementationSupplierException e) {
            DataKeyGenerationException wrapper = new DataKeyGenerationException(correlationId);
            LOGGER.error(wrapper.getMessage(), e);
            throw wrapper;
        } finally {
            loginManager.logout();
        }
    }

    @Override
    public boolean canSeeDependencies() {
        return this.cryptoImplementationSupplier != null;
    }

    private final CryptoImplementationSupplier cryptoImplementationSupplier;
}
