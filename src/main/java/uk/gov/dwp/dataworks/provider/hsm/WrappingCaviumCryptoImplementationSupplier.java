package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.cfm2.CFM2Exception;
import com.cavium.cfm2.Util;
import com.cavium.key.CaviumKey;
import com.cavium.key.CaviumKeyAttributes;
import com.cavium.key.CaviumRSAPrivateKey;
import com.cavium.key.CaviumRSAPublicKey;
import com.cavium.key.parameter.CaviumAESKeyGenParameterSpec;
import com.cavium.provider.CaviumProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.GarbledDataKeyException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;

import javax.crypto.*;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.IOException;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import static uk.gov.dwp.dataworks.provider.hsm.HsmDataKeyDecryptionConstants.*;

@Component
@Profile("WrappingCavium")
public class WrappingCaviumCryptoImplementationSupplier implements CryptoImplementationSupplier {

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
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_KEY_TYPE, CAVIUM_PROVIDER);
            CaviumAESKeyGenParameterSpec aesSpec =
                    new CaviumAESKeyGenParameterSpec(128, DATA_KEY_LABEL, EXTRACTABLE, NOT_PERSISTENT);
            keyGenerator.init(aesSpec);
            return keyGenerator.generateKey();
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            LOGGER.error("Failed to create data key", e);
            throw new CryptoImplementationSupplierException(e);
        }
    }


        @Override
        public byte[] encryptedKey(Integer wrappingKeyHandle, Key dataKey)
                throws CryptoImplementationSupplierException, MasterKeystoreException {
            try {
                LOGGER.info("wrappingKeyHande: '{}'.", wrappingKeyHandle);
                byte[] keyAttribute = Util.getKeyAttributes(wrappingKeyHandle);
                CaviumRSAPublicKey publicKey = new CaviumRSAPublicKey(wrappingKeyHandle,  new CaviumKeyAttributes(keyAttribute));
                LOGGER.info("Public key bytes: '{}'.", new String(Base64.getEncoder().encode(publicKey.getEncoded())));
                OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
                Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256ANDMGF1Padding", "Cavium");
                cipher.init(Cipher.WRAP_MODE, publicKey, spec);
                return Base64.getEncoder().encode(cipher.wrap(dataKey));
            }
            catch (NoSuchAlgorithmException | NoSuchProviderException |
                    NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
                throw new CryptoImplementationSupplierException(e);
            }
            catch (CFM2Exception e) {
                String message = "Failed to encrypt key, retry will be attempted unless max attempts reached";
                LOGGER.warn(message);
                throw new MasterKeystoreException(message, e);
            }
        }

        @Override
    public String decryptedKey(Integer decryptionKeyHandle, String ciphertextDataKey)
            throws CryptoImplementationSupplierException, MasterKeystoreException {
        try {
            LOGGER.info("decryptionKeyHandle: '{}'.", decryptionKeyHandle);
            OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT);
            byte[] privateKeyAttribute = Util.getKeyAttributes(decryptionKeyHandle);
            CaviumKeyAttributes privateAttributes = new CaviumKeyAttributes(privateKeyAttribute);
            CaviumRSAPrivateKey privateKey = new CaviumRSAPrivateKey(decryptionKeyHandle, privateAttributes);
            Cipher cipher = Cipher.getInstance(cipherTransformation, CAVIUM_PROVIDER);
            cipher.init(Cipher.UNWRAP_MODE, privateKey, spec);
            byte[] decodedCipher = Base64.getDecoder().decode(ciphertextDataKey.getBytes());
            Key unwrappedKey = cipher.unwrap(decodedCipher, "AES", Cipher.SECRET_KEY);
            if (unwrappedKey != null) {
                byte[] exportedUnwrappedKey = unwrappedKey.getEncoded();
                if (exportedUnwrappedKey != null) {
                    LOGGER.debug("Removing unwrapped session key.");
                    cleanupKey(unwrappedKey);
                    return new String(Base64.getEncoder().encode(exportedUnwrappedKey));
                }
                else {
                    LOGGER.warn("Exported unwrapped key is null, unwrappedKey: '{}'", unwrappedKey);
                    throw new GarbledDataKeyException();
                }
            }
            else {
                LOGGER.warn("Unwrapped key is null.");
                throw new GarbledDataKeyException();
            }
        }
        catch (NoSuchPaddingException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new CryptoImplementationSupplierException(e);
        }
        catch (InvalidKeyException e) {
            LOGGER.warn("Invalid key: {}", e.getMessage(), e);
            throw new GarbledDataKeyException();
        }
        catch (CFM2Exception e) {
            String message = "Failed to decrypt key, retry will be attempted unless max attempts reached";
            LOGGER.warn("Failed to decrypt key: '{}', '{}', '{}'", e.getMessage(), e.getStatus(), e.getClass().getSimpleName());
            LOGGER.warn(message);
            throw new MasterKeystoreException(message, e);
        }
    }

    @Override
    public void cleanupKey(Key datakey) {
        try {
            LOGGER.debug("Deleting session key.");
            Util.deleteKey((CaviumKey) datakey);
        }
        catch (CFM2Exception e) {
            LOGGER.error("Failed to delete datakey: '" + e.getMessage() + "'", e);
        }

    }

    @Value("${cipher.transformation:RSA/ECB/OAEPWithSHA-256ANDMGF1Padding}")
    private String cipherTransformation;

    private final static Logger LOGGER = LoggerFactory.getLogger(WrappingCaviumCryptoImplementationSupplier.class);
}
