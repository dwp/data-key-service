package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.key.CaviumKey;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.provider.LoginManager;

import java.util.Base64;

@Service
@Profile("HSM")
public class HsmDataKeyGeneratorProvider extends HsmDependent implements DataKeyGeneratorProvider {

    public HsmDataKeyGeneratorProvider(CurrentKeyIdProvider currentKeyIdProvider,
            LoginManager loginManager,
            CryptoImplementationSupplier cryptoImplementationSupplier) {
        super(loginManager);
        this.cryptoImplementationSupplier = cryptoImplementationSupplier;
        this.currentKeyIdProvider = currentKeyIdProvider;
    }

    @Override
    public GenerateDataKeyResponse generateDataKey(String keyId) throws DataKeyGenerationException {
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
            throw new DataKeyGenerationException();
        }
        finally {
            loginManager.logout();
        }
    }

    @Override
    public boolean canSeeDependencies() {
        String currentKeyId = this.currentKeyIdProvider.getKeyId();
        return generateDataKey(currentKeyId) != null;
    }

    private final CurrentKeyIdProvider currentKeyIdProvider;
    private final CryptoImplementationSupplier cryptoImplementationSupplier;
}
