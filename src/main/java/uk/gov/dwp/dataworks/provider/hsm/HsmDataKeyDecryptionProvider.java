package uk.gov.dwp.dataworks.provider.hsm;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionException;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.provider.LoginManager;

@Service
@Profile("HSM")
public class HsmDataKeyDecryptionProvider extends HsmDependent
        implements DataKeyDecryptionProvider, HsmDataKeyDecryptionConstants {

    HsmDataKeyDecryptionProvider(CurrentKeyIdProvider currentKeyIdProvider,
            DataKeyGeneratorProvider dataKeyGeneratorProvider,
            LoginManager loginManager,
            CryptoImplementationSupplier cryptoImplementationSupplier) {
        super(loginManager);
        this.cryptoImplementationSupplier = cryptoImplementationSupplier;
        this.currentKeyIdProvider = currentKeyIdProvider;
        this.dataKeyGeneratorProvider = dataKeyGeneratorProvider;
    }

    @Override
    public DecryptDataKeyResponse decryptDataKey(String decryptionKeyId, String ciphertextDataKey) {
        try {
            loginManager.login();
            Integer decryptionKeyHandle = privateKeyHandle(decryptionKeyId);
            String decryptedKey = cryptoImplementationSupplier.decryptedKey(decryptionKeyHandle, ciphertextDataKey);
            return new DecryptDataKeyResponse(decryptionKeyId, decryptedKey);
        }
        catch (CryptoImplementationSupplierException e) {
            throw new DataKeyDecryptionException();
        }
        finally {
            loginManager.logout();
        }
    }

    @Override
    public boolean canSeeDependencies() {
        String currentKeyId = this.currentKeyIdProvider.getKeyId();
        GenerateDataKeyResponse generateDataKeyResponse = this.dataKeyGeneratorProvider.generateDataKey(currentKeyId);
        DecryptDataKeyResponse decryptDataKeyResponse = decryptDataKey(currentKeyId, generateDataKeyResponse.ciphertextDataKey);
        return decryptDataKeyResponse.plaintextDataKey.equals(generateDataKeyResponse.plaintextDataKey);
    }

    private final CurrentKeyIdProvider currentKeyIdProvider;
    private final DataKeyGeneratorProvider dataKeyGeneratorProvider;
    private final CryptoImplementationSupplier cryptoImplementationSupplier;
}
