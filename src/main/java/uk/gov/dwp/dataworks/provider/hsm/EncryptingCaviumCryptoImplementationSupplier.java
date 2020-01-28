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
import java.io.IOException;
import java.security.*;
import java.util.Base64;

import static uk.gov.dwp.dataworks.provider.hsm.HsmDataKeyDecryptionConstants.*;

@Component
@Profile("EncryptingCavium")
public class EncryptingCaviumCryptoImplementationSupplier implements CryptoImplementationSupplier {

    static {
        try {
            Security.addProvider(new CaviumProvider());
        } catch (IOException e) {
            throw new RuntimeException("Cavium provider not available: '" + e.getMessage() + "'", e);
        }
    }

    @Override
    public Key dataKey(String correlationId) throws CryptoImplementationSupplierException {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_KEY_TYPE, CAVIUM_PROVIDER);
            CaviumAESKeyGenParameterSpec aesSpec =
                    new CaviumAESKeyGenParameterSpec(128, DATA_KEY_LABEL, EXTRACTABLE, NOT_PERSISTENT);
            keyGenerator.init(aesSpec);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            LOGGER.error("Failed to create data key", e);
            throw new CryptoImplementationSupplierException(e);
        }
    }

    @Override
    public byte[] encryptedKey(Integer wrappingKeyHandle, Key dataKey, String correlationId)
            throws CryptoImplementationSupplierException, MasterKeystoreException {
        try {
            LOGGER.info("wrappingKeyHandle: '{}'.", wrappingKeyHandle);
            byte[] keyAttribute = Util.getKeyAttributes(wrappingKeyHandle);
            CaviumRSAPublicKey publicKey = new CaviumRSAPublicKey(wrappingKeyHandle, new CaviumKeyAttributes(keyAttribute));
            LOGGER.info("Public key bytes: '{}'.", new String(Base64.getEncoder().encode(publicKey.getEncoded())));
            Cipher cipher = Cipher.getInstance(cipherTransformation, CAVIUM_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encode(cipher.doFinal(dataKey.getEncoded()));
        } catch (BadPaddingException | NoSuchAlgorithmException | NoSuchProviderException |
                NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException e) {
            throw new CryptoImplementationSupplierException(e);
        } catch (CFM2Exception e) {
            String message = "Failed to encrypt key, retry will be attempted unless max attempts reached. correlation_id: " + correlationId;
            LOGGER.warn(message);
            throw new MasterKeystoreException(message, e);
        }
    }

    @Override
    public String decryptedKey(Integer decryptionKeyHandle, String ciphertextDataKey, String correlationId)
            throws CryptoImplementationSupplierException, MasterKeystoreException {
        try {
            LOGGER.info("decryptionKeyHandle: '{}'. correlation_id: {}", decryptionKeyHandle, correlationId);
            byte[] privateKeyAttribute = Util.getKeyAttributes(decryptionKeyHandle);
            CaviumKeyAttributes privateAttributes = new CaviumKeyAttributes(privateKeyAttribute);
            CaviumRSAPrivateKey privateKey = new CaviumRSAPrivateKey(decryptionKeyHandle, privateAttributes);
            Cipher cipher = Cipher.getInstance(cipherTransformation, CAVIUM_PROVIDER);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decodedCipher = Base64.getDecoder().decode(ciphertextDataKey.getBytes());
            byte[] decrypted = cipher.doFinal(decodedCipher);
            if (decrypted != null) {
                return new String(Base64.getEncoder().encode(decrypted));
            } else {
                throw new GarbledDataKeyException(correlationId);
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | NoSuchProviderException | IllegalBlockSizeException | BadPaddingException e) {
            throw new CryptoImplementationSupplierException(e);
        } catch (InvalidKeyException e) {
            throw new GarbledDataKeyException(correlationId);
        } catch (CFM2Exception e) {
            LOGGER.warn("Failed to decrypt key: '{}', '{}', '{}'", e.getMessage(), e.getStatus(), e.getClass().getSimpleName());
            String message = "Failed to decrypt key, retry will be attempted unless max attempts reached. correlation_id: " + correlationId;
            LOGGER.warn(message);
            throw new MasterKeystoreException(message, e);
        }
    }

    @Override
    public void cleanupKey(Key datakey) {
        try {
            LOGGER.debug("Deleting session key.");
            Util.deleteKey((CaviumKey) datakey);
        } catch (CFM2Exception e) {
            LOGGER.error("Failed to delete datakey: '" + e.getMessage() + "'", e);
        }

    }

    @Value("${cipher.transformation:RSA/ECB/OAEPWithSHA-256ANDMGF1Padding}")
    private String cipherTransformation;

    private final static Logger LOGGER = LoggerFactory.getLogger(EncryptingCaviumCryptoImplementationSupplier.class);
}
