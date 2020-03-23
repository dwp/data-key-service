package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.cfm2.CFM2Exception;
import com.cavium.cfm2.Util;
import com.cavium.key.CaviumKey;
import com.cavium.key.CaviumKeyAttributes;
import com.cavium.key.CaviumRSAPrivateKey;
import com.cavium.key.CaviumRSAPublicKey;
import com.cavium.key.parameter.CaviumAESKeyGenParameterSpec;
import com.cavium.provider.CaviumProvider;
import kotlin.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.GarbledDataKeyException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.logging.DataworksLogger;

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
public class CaviumCryptoImplementationSupplier implements CryptoImplementationSupplier {

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
            LOGGER.error("Failed to create data key", e,  new Pair<>("correlation_id", correlationId));
            throw new CryptoImplementationSupplierException(e);
        }
    }

    @Override
    public byte[] encryptedKey(Integer wrappingKeyHandle, Key dataKey, String correlationId)
            throws CryptoImplementationSupplierException, MasterKeystoreException {
        try {

            byte[] keyAttribute = Util.getKeyAttributes(wrappingKeyHandle);
            CaviumRSAPublicKey publicKey = new CaviumRSAPublicKey(wrappingKeyHandle, new CaviumKeyAttributes(keyAttribute));
            String encoded = new String(Base64.getEncoder().encode(publicKey.getEncoded()));

            LOGGER.info("Encrypting key",
                    new Pair<>("wrapping_key_handle", wrappingKeyHandle.toString()),
                    new Pair<>("public_key", encoded),
                    new Pair<>("correlation_id", correlationId));

            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encode(cipher.doFinal(dataKey.getEncoded()));
        } catch (BadPaddingException | NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            throw new CryptoImplementationSupplierException(e);
        } catch (CFM2Exception e) {
            String message = "Failed to encrypt key, retry will be attempted unless max attempts reached. correlation_id: " + correlationId;
            LOGGER.warn(message, new Pair<>("correlation_id", correlationId));
            throw new MasterKeystoreException(message, e);
        }
    }

    @Override
    public String decryptedKey(Integer decryptionKeyHandle, String ciphertextDataKey, String correlationId)
            throws CryptoImplementationSupplierException, MasterKeystoreException {
        try {
            LOGGER.info("Decrypting key",
                    new Pair<>("decryption_key_handle", decryptionKeyHandle.toString()),
                    new Pair<>("correlation_id", correlationId));
            byte[] privateKeyAttribute = Util.getKeyAttributes(decryptionKeyHandle);
            CaviumKeyAttributes privateAttributes = new CaviumKeyAttributes(privateKeyAttribute);
            CaviumRSAPrivateKey privateKey = new CaviumRSAPrivateKey(decryptionKeyHandle, privateAttributes);
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, privateKey);
            byte[] decodedCipher = Base64.getDecoder().decode(ciphertextDataKey.getBytes());
            byte[] decrypted = cipher.doFinal(decodedCipher);
            if (decrypted != null) {
                return new String(Base64.getEncoder().encode(decrypted));
            } else {
                throw new GarbledDataKeyException(correlationId);
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | NoSuchProviderException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new CryptoImplementationSupplierException(e);
        } catch (InvalidKeyException e) {
            throw new GarbledDataKeyException(correlationId);
        } catch (CFM2Exception e) {
            LOGGER.error("Failed to decrypt key", e, new Pair<>("correlation_id", correlationId));
            String message = "Failed to decrypt key, retry will be attempted unless max attempts reached. correlation_id: " + correlationId;
            throw new MasterKeystoreException(message, e);
        }
    }

    private Cipher cipher(int mode, Key key) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(cipherTransformation, CAVIUM_PROVIDER);
        cipher.init(mode, key, oaepParameterSpec());
        return cipher;
    }

    private OAEPParameterSpec oaepParameterSpec() {
        return new OAEPParameterSpec(hashingAlgorithm, maskGenerationAlgorithm,
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT);
    }

    @Value("${cipher.transformation:RSA/ECB/OAEPWithSHA-256ANDMGF1Padding}")
    private String cipherTransformation;

    @Value("${hashing.algorithm:SHA-256}")
    private String hashingAlgorithm;

    @Value("${mask.generation.algorithm:MGF1}")
    private String maskGenerationAlgorithm;

    @Override
    public void cleanupKey(Key datakey) {
        try {
            LOGGER.debug("Deleting session key.");
            Util.deleteKey((CaviumKey) datakey);
        } catch (CFM2Exception e) {
            LOGGER.error("Failed to delete datakey: '" + e.getMessage() + "'", e);
        }

    }
    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(CaviumCryptoImplementationSupplier.class.toString());
}
