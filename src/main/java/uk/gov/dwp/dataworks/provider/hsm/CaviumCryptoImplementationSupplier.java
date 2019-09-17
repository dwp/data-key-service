package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.cfm2.CFM2Exception;
import com.cavium.cfm2.Util;
import com.cavium.key.CaviumKey;
import com.cavium.key.CaviumKeyAttributes;
import com.cavium.key.CaviumRSAPrivateKey;
import com.cavium.key.CaviumRSAPublicKey;
import com.cavium.key.parameter.CaviumAESKeyGenParameterSpec;
import com.cavium.key.parameter.CaviumKeyGenAlgorithmParameterSpec;
import com.cavium.provider.CaviumProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.io.IOException;
import java.security.*;
import java.util.Base64;

@Component
@Profile("HSM")
public class CaviumCryptoImplementationSupplier implements CryptoImplementationSupplier, HsmDataKeyDecryptionConstants {

    static {
        try {
            Security.addProvider(new CaviumProvider());
        }
        catch (IOException e) {
            throw new RuntimeException("Cavium provider not available: '" + e.getMessage() + "'", e);
        }
    }

    @Override
    public Key dataKey() throws CryptoImplementationSupplierException {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_KEY_TYPE, PROVIDER);
            CaviumAESKeyGenParameterSpec aesSpec =
                    new CaviumAESKeyGenParameterSpec(128, DATA_KEY_LABEL, EXTRACTABLE, NOT_PERSISTENT);
            keyGenerator.init(aesSpec);
            return keyGenerator.generateKey();
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new CryptoImplementationSupplierException(e);
        }
    }

    @Override
    public byte[] encryptedKey(Integer wrappingKeyHandle, Key dataKey) throws CryptoImplementationSupplierException {
        try {
            byte[] keyAttribute = Util.getKeyAttributes(wrappingKeyHandle);
            CaviumRSAPublicKey publicKey = new CaviumRSAPublicKey(wrappingKeyHandle,  new CaviumKeyAttributes(keyAttribute));
            byte[] wrappedKey = Util.rsaWrapKey(publicKey, (CaviumKey) dataKey, PADDING);
            return Base64.getEncoder().encode(wrappedKey);
        }
        catch (CFM2Exception | InvalidKeyException e) {
            throw new CryptoImplementationSupplierException(e);
        }
    }

    @Override
    public String decryptedKey(Integer decryptionKeyHandle, String ciphertextDataKey) throws CryptoImplementationSupplierException {
        try {
            byte[] privateKeyAttribute = Util.getKeyAttributes(decryptionKeyHandle);
            CaviumKeyAttributes privateAttributes = new CaviumKeyAttributes(privateKeyAttribute);
            CaviumRSAPrivateKey privateKey = new CaviumRSAPrivateKey(decryptionKeyHandle, privateAttributes);
            CaviumKeyGenAlgorithmParameterSpec unwrappingSpec = new
                    CaviumKeyGenAlgorithmParameterSpec(DATA_KEY_LABEL, EXTRACTABLE, NOT_PERSISTENT);
            byte[] decodedCipher = Base64.getDecoder().decode(ciphertextDataKey.getBytes());

            CaviumKey unwrappedKey =
                    Util.rsaUnwrapKey(privateKey,
                            decodedCipher,
                            SYMMETRIC_KEY_TYPE,
                            Cipher.SECRET_KEY,
                            unwrappingSpec, PADDING);
            byte[] exportedUnwrappedKey = unwrappedKey.getEncoded();
            return new String(Base64.getEncoder().encode(exportedUnwrappedKey));
        }
        catch (CFM2Exception | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoImplementationSupplierException(e);
        }
    }
}
