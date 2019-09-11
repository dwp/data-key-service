package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.key.CaviumKey;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

import java.util.Base64;

@Service
@Profile("HSM")
public class HsmDataKeyGeneratorProvider extends HsmDependent implements DataKeyGeneratorProvider {

    public HsmDataKeyGeneratorProvider(CryptoImplementationSupplier cryptoImplementationSupplier) {
        this.cryptoImplementationSupplier = cryptoImplementationSupplier;
    }

    @Override
    public GenerateDataKeyResponse generateDataKey(String keyId) throws DataKeyGenerationException {
        try {
            int publicKeyHandle = publicKeyHandle(keyId);
            System.err.println("publicKeyHandle: '" + publicKeyHandle + "'");
            CaviumKey dataKey = (CaviumKey) cryptoImplementationSupplier.dataKey();
            byte[] plaintextDatakey = Base64.getEncoder().encode(dataKey.getEncoded());
            byte[] ciphertext = cryptoImplementationSupplier.encryptedKey(publicKeyHandle, dataKey);
            return new GenerateDataKeyResponse(keyId,
                                                new String(plaintextDatakey),
                                                new String(ciphertext));
        }
        catch (CryptoImplementationSupplierException e) {
            e.printStackTrace(System.err);
            throw new DataKeyGenerationException();
        }
    }

    @Override
    public boolean canSeeDependencies() {
        return false;
    }

    private final CryptoImplementationSupplier cryptoImplementationSupplier;
}
