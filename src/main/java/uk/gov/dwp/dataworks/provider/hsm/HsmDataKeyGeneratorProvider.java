package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.key.CaviumKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.provider.HsmLoginManager;

import java.util.Base64;

@Service
@Profile("HSM")
public class HsmDataKeyGeneratorProvider extends HsmDependent implements DataKeyGeneratorProvider {

    private final int MAX_ATTEMPTS = 10;
    private final static Logger LOGGER = LoggerFactory.getLogger(HsmDataKeyGeneratorProvider.class);

    public HsmDataKeyGeneratorProvider(HsmLoginManager loginManager,
            CryptoImplementationSupplier cryptoImplementationSupplier) {
        super(loginManager);
        this.cryptoImplementationSupplier = cryptoImplementationSupplier;
    }

    @Override
    @Retryable(
            value = { MasterKeystoreException.class },
            maxAttempts = MAX_ATTEMPTS,
            backoff = @Backoff(delay = 1_000))
    public GenerateDataKeyResponse generateDataKey(String keyId)
            throws DataKeyGenerationException, MasterKeystoreException {
        try {
            loginManager.login();
            int publicKeyHandle = publicKeyHandle(keyId);
            CaviumKey dataKey = (CaviumKey) cryptoImplementationSupplier.dataKey();
            byte[] plaintextDatakey = Base64.getEncoder().encode(dataKey.getEncoded());
            byte[] ciphertext = cryptoImplementationSupplier.encryptedKey(publicKeyHandle, dataKey);
            return new GenerateDataKeyResponse(keyId,
                                                new String(plaintextDatakey),
                                                new String(ciphertext));
        }
        catch (CryptoImplementationSupplierException e) {
            LOGGER.error("Failed to generate a new data key due to an internal error. Try again later.", e);
            throw new DataKeyGenerationException();
        }
        finally {
            loginManager.logout();
        }
    }

    @Override
    public boolean canSeeDependencies() {
        return this.cryptoImplementationSupplier != null;
    }
    private final CryptoImplementationSupplier cryptoImplementationSupplier;
}
