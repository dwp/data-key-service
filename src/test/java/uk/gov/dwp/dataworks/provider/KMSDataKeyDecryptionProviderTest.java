package uk.gov.dwp.dataworks.provider;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionException;
import uk.gov.dwp.dataworks.errors.GarbledDataKeyException;
import uk.gov.dwp.dataworks.errors.UnusableParameterException;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles({"UnitTest", "KMS"})
public class KMSDataKeyDecryptionProviderTest {

    @Before
    public void init() {
        Mockito.reset(awsKms);
    }

    @Test
    public void canDecryptDataKey() {
        String encryptedDataKey = "ENCRYPTEDDATAKEY";
        String plainTextDataKey = "PLAINTEXTDATAKEY";
        String dataKeyEncryptionKeyId = "DATAKEYENCRYPTIONKEYID";
        Base64.Encoder encoder = Base64.getEncoder();
        DecryptDataKeyResponse expected =
                new DecryptDataKeyResponse(dataKeyEncryptionKeyId,
                        encoder.encodeToString(plainTextDataKey.getBytes()));
        DecryptResult result = new DecryptResult();
        result.setKeyId(dataKeyEncryptionKeyId);
        result.setPlaintext(ByteBuffer.wrap(plainTextDataKey.getBytes()));
        given(awsKms.decrypt(ArgumentMatchers.any(DecryptRequest.class))).willReturn(result);
        DecryptDataKeyResponse actual =
                kmsDataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, encryptedDataKey);
        assertEquals(expected, actual);
    }

    @Test(expected = UnusableParameterException.class)
    public void wontDecryptOverMaxSizePayload() {
        byte[] largeAmountOfCypherText = new byte[DataKeyDecryptionProvider.MAX_PAYLOAD_SIZE + 1];
        Arrays.fill(largeAmountOfCypherText, (byte) 48);
        String decryptionArg = new String(largeAmountOfCypherText);
        kmsDataKeyDecryptionProvider.decryptDataKey("DATAKEYENCRYPTIONKEYID", decryptionArg);
    }

    @Test
    public void willDecryptMaxSizePayload() {
        byte[] largeAmountOfCypherText = new byte[DataKeyDecryptionProvider.MAX_PAYLOAD_SIZE];
        Arrays.fill(largeAmountOfCypherText, (byte) 48);
        String decryptionArg = new String(largeAmountOfCypherText);
        String plainTextDataKey = "PLAINTEXTDATAKEY";
        String dataKeyEncryptionKeyId = "DATAKEYENCRYPTIONKEYID";
        Base64.Encoder encoder = Base64.getEncoder();
        DecryptDataKeyResponse expected =
                new DecryptDataKeyResponse(dataKeyEncryptionKeyId,
                        encoder.encodeToString(plainTextDataKey.getBytes()));
        DecryptResult result = new DecryptResult();
        result.setKeyId(dataKeyEncryptionKeyId);
        result.setPlaintext(ByteBuffer.wrap(plainTextDataKey.getBytes()));
        given(awsKms.decrypt(ArgumentMatchers.any(DecryptRequest.class))).willReturn(result);
        DecryptDataKeyResponse actual =
                kmsDataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, decryptionArg);
        assertEquals(expected, actual);
    }

    @Test
    public void handlesInvalidCiphertextException() {
        try {
            throwException(InvalidCiphertextException.class);
        }
        catch (GarbledDataKeyException ex) {
            assertEquals("The supplied data key could not be decrypted. Either the ciphertext is invalid or the data key encryption key is incorrect.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + GarbledDataKeyException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesIllegalArgumentException() {
        try {
            throwException(IllegalArgumentException.class);
        }
        catch (GarbledDataKeyException ex) {
            assertEquals("The supplied data key could not be decrypted. Either the ciphertext is invalid or the data key encryption key is incorrect.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + GarbledDataKeyException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesNotFoundException() {
        try {
            throwException(NotFoundException.class);
        }
        catch (DataKeyDecryptionException ex) {
            assertEquals("Failed to decrypt this data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyDecryptionException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesDisabledException() {
        try {
            throwException(DisabledException.class);
        }
        catch (DataKeyDecryptionException ex) {
            assertEquals("Failed to decrypt this data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyDecryptionException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesKeyUnavailableException() {
        try {
            throwException(DisabledException.class);
        }
        catch (DataKeyDecryptionException ex) {
            assertEquals("Failed to decrypt this data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyDecryptionException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesDependencyTimeoutException() {
        try {
            throwException(DependencyTimeoutException.class);
        }
        catch (DataKeyDecryptionException ex) {
            assertEquals("Failed to decrypt this data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyDecryptionException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesInvalidGrantTokenException() {
        try {
            throwException(InvalidGrantTokenException.class);
        }
        catch (DataKeyDecryptionException ex) {
            assertEquals("Failed to decrypt this data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyDecryptionException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesKMSInternalException() {
        try {
            throwException(KMSInternalException.class);
        }
        catch (DataKeyDecryptionException ex) {
            assertEquals("Failed to decrypt this data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyDecryptionException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesKMSInvalidStateException() {
        try {
            throwException(KMSInternalException.class);
        }
        catch (DataKeyDecryptionException ex) {
            assertEquals("Failed to decrypt this data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyDecryptionException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesRuntimeException() {
        try {
            throwException(RuntimeException.class);
        }
        catch (DataKeyDecryptionException ex) {
            assertEquals("Failed to decrypt this data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyDecryptionException.class + " got " + e.getClass() + ".");
        }
    }

    public void throwException(Class<? extends Throwable> exceptionClass) {
        String encryptedDataKey = "ENCRYPTEDDATAKEY";
        String dataKeyEncryptionKeyId = "DATAKEYENCRYPTIONKEYID";
        given(awsKms.decrypt(ArgumentMatchers.any(DecryptRequest.class))).willThrow(exceptionClass);
        kmsDataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, encryptedDataKey);
    }

    @Autowired
    private KMSDataKeyDecryptionProvider kmsDataKeyDecryptionProvider;

    @Autowired
    private AWSKMS awsKms;
}