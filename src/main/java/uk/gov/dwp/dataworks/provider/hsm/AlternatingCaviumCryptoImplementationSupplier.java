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
import uk.gov.dwp.dataworks.errors.UnusableParameterException;

import javax.crypto.*;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.IOException;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import static uk.gov.dwp.dataworks.provider.hsm.HsmDataKeyDecryptionConstants.*;

@Component
@Profile("Cavium")
public class AlternatingCaviumCryptoImplementationSupplier implements CryptoImplementationSupplier {

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
            LOGGER.error("Failed to create data key. correlation_id: " + correlationId, e);
            throw new CryptoImplementationSupplierException(e);
        }
    }

    @Override
    public byte[] encryptedKey(Integer wrappingKeyHandle, Key dataKey, String correlationId)
            throws CryptoImplementationSupplierException, MasterKeystoreException {
        try {
            CaviumRSAPublicKey publicKey = publicKey(wrappingKeyHandle);
            String key = new String(Base64.getEncoder().encode(publicKey.getEncoded()));
            LOGGER.info("Public key bytes: '{}'. correlation_id: {}", key, correlationId);
            Cipher cipher = bouncyCastleCompatibleCipher(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encode(cipher.doFinal(dataKey.getEncoded()));
        } catch (BadPaddingException | NoSuchAlgorithmException | NoSuchProviderException |
                NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
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
            CaviumRSAPrivateKey privateKey = privateKey(decryptionKeyHandle, correlationId);
            Cipher jceCipher = sunJceCompatibleCipher(Cipher.DECRYPT_MODE, privateKey);

            try {
                LOGGER.info("Trying SunJCE compatible cipher.");
                return decryptedKey(jceCipher, ciphertextDataKey, correlationId);
            } catch (Exception e) {
                try {
                    Cipher bcCipher = bouncyCastleCompatibleCipher(Cipher.DECRYPT_MODE, privateKey);
                    LOGGER.info("SunJCE failed trying bouncy castle compatible cipher.");
                    return decryptedKey(bcCipher, ciphertextDataKey, correlationId);
                } catch (Exception e2) {
                    throw new GarbledDataKeyException(correlationId);
                }
            }
        } catch (InvalidKeyException |
                NoSuchPaddingException |
                NoSuchAlgorithmException |
                NoSuchProviderException |
                InvalidAlgorithmParameterException e) {
            throw new CryptoImplementationSupplierException(e);
        }
    }

    public String decryptedKey(Cipher cipher, String ciphertextDataKey, String correlationId) {
        try {
            LOGGER.info("Decrypting with '{}/{}/{}'. correlation_id: {}", cipher.getProvider(), cipher.getAlgorithm(), cipher.getParameters(), correlationId);
            byte[] decodedCipher = Base64.getDecoder().decode(ciphertextDataKey.getBytes());
            byte[] decrypted = cipher.doFinal(decodedCipher);
            if (decrypted != null) {
                return new String(Base64.getEncoder().encode(decrypted));
            } else {
                LOGGER.warn("Decrypting key '{}' yielded null. correlation_id: {}", ciphertextDataKey, correlationId);
                throw new GarbledDataKeyException(correlationId);
            }
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            LOGGER.warn("Failed to decrypt key: '{}'. correlation_id: {}", e.getMessage(), correlationId);
            throw new GarbledDataKeyException(correlationId);
        }
    }

    private Cipher sunJceCompatibleCipher(int mode, Key key) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException {
        return compatibleCipher(sunJceOaepParameterSpec(), mode, key);
    }

    private Cipher bouncyCastleCompatibleCipher(int mode, Key key) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException {
        return compatibleCipher(bcOaepParameterSpec(), mode, key);
    }

    private Cipher compatibleCipher(OAEPParameterSpec spec, int mode, Key key) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(cipherTransformation, CAVIUM_PROVIDER);
        cipher.init(mode, key, spec);
        return cipher;
    }

    private OAEPParameterSpec sunJceOaepParameterSpec() {
        return oaepParameterSpec(MGF1ParameterSpec.SHA1);
    }

    private OAEPParameterSpec bcOaepParameterSpec() {
        return oaepParameterSpec(MGF1ParameterSpec.SHA256);
    }

    private OAEPParameterSpec oaepParameterSpec(MGF1ParameterSpec mgf1ParameterSpec) {
        return new OAEPParameterSpec("SHA-256", "MGF1",
                mgf1ParameterSpec,
                PSource.PSpecified.DEFAULT);
    }

    private CaviumRSAPrivateKey privateKey(Integer decryptionKeyHandle, String correlationId) throws CryptoImplementationSupplierException {
        try {
            LOGGER.info("decryptionKeyHandle: '{}'. correlation_id: {}", decryptionKeyHandle, correlationId);
            byte[] privateKeyAttribute = Util.getKeyAttributes(decryptionKeyHandle);
            CaviumKeyAttributes privateAttributes = new CaviumKeyAttributes(privateKeyAttribute);
            return new CaviumRSAPrivateKey(decryptionKeyHandle, privateAttributes);
        } catch (CFM2Exception e) {
            throw new UnusableParameterException(correlationId);
        }
    }

    private CaviumRSAPublicKey publicKey(Integer wrappingKeyHandle) throws CFM2Exception {
        LOGGER.info("publicKeyHandle: '{}'.", wrappingKeyHandle);
        byte[] keyAttribute = Util.getKeyAttributes(wrappingKeyHandle);
        return new CaviumRSAPublicKey(wrappingKeyHandle, new CaviumKeyAttributes(keyAttribute));
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
