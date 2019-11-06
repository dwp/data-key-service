package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.cfm2.CFM2Exception;
import com.cavium.cfm2.Util;
import com.cavium.key.CaviumKey;
import com.cavium.key.CaviumKeyAttributes;
import com.cavium.key.CaviumRSAPrivateKey;
import com.cavium.key.CaviumRSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.GarbledDataKeyException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

@Component
@Profile("Cavium")
public class EncryptingCaviumCryptoImplementationSupplier implements CryptoImplementationSupplier {

    @Override
    public Key dataKey() throws CryptoImplementationSupplierException {
        return null;
    }

    @Override
    public byte[] encryptedKey(Integer wrappingKeyHandle, Key dataKey)
            throws CryptoImplementationSupplierException, MasterKeystoreException {
        try {
            byte[] keyAttribute = Util.getKeyAttributes(wrappingKeyHandle);
            CaviumRSAPublicKey publicKey = new CaviumRSAPublicKey(wrappingKeyHandle,  new CaviumKeyAttributes(keyAttribute));
            OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256ANDMGF1Padding", "Cavium");
            cipher.init(Cipher.WRAP_MODE, publicKey, spec);
            return cipher.wrap(dataKey);
//            cipher.init(Cipher.WRAP_MODE, publicKey, spec);
//            return cipher.doFinal(dataKey.getEncoded());
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
            OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            byte[] privateKeyAttribute = Util.getKeyAttributes(decryptionKeyHandle);
            CaviumKeyAttributes privateAttributes = new CaviumKeyAttributes(privateKeyAttribute);
            CaviumRSAPrivateKey privateKey = new CaviumRSAPrivateKey(decryptionKeyHandle, privateAttributes);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256ANDMGF1Padding", "SunJCE");
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
                    throw new GarbledDataKeyException();
                }
            }
            else {
                throw new GarbledDataKeyException();
            }
        }
        catch (NoSuchPaddingException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new CryptoImplementationSupplierException(e);
        }
        catch (InvalidKeyException e) {
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

    private final static Logger LOGGER = LoggerFactory.getLogger(ExplicitHsmLoginManager.class);

}
