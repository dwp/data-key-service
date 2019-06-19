package uk.gov.dwp.dataworks.provider;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;

import java.nio.ByteBuffer;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles({"UnitTest", "KMS"})
public class KMSDataKeyGeneratorProviderTest {

    @Test
    public void generateDataKey() {
        String dataKeyEncryptionKeyId = "DATAKEYENCRYPTIONKEYID";
        String plainTextKey = "PLAINTEXTKEY";
        String encryptedDataKey = "ENCRYPTEDDATAKEY";
        GenerateDataKeyResult result = new GenerateDataKeyResult();
        result.setKeyId(dataKeyEncryptionKeyId);
        result.setPlaintext(ByteBuffer.wrap(plainTextKey.getBytes()));
        result.setCiphertextBlob(ByteBuffer.wrap(encryptedDataKey.getBytes()));
        GenerateDataKeyRequest generateDataKeyRequest = new GenerateDataKeyRequest();
        generateDataKeyRequest.setKeyId(dataKeyEncryptionKeyId);
        generateDataKeyRequest.setKeySpec("AES_128");
        given(awsKms.generateDataKey(generateDataKeyRequest)).willReturn(result);
        Base64.Encoder encoder = Base64.getEncoder();

        GenerateDataKeyResponse expected =
                new GenerateDataKeyResponse(dataKeyEncryptionKeyId,
                         encoder.encodeToString(plainTextKey.getBytes()),
                         encoder.encodeToString(encryptedDataKey.getBytes()));

        GenerateDataKeyResponse actual =
                dataKeyGeneratorProvider.generateDataKey(dataKeyEncryptionKeyId);

        assertEquals(expected, actual);
    }


    @Test
    public void handlesNotFoundException() {
        try {
            throwException(NotFoundException.class);
        }
        catch (DataKeyGenerationException ex) {
            assertEquals("Failed to generate a new data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyGenerationException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesDisabledException() {
        try {
            throwException(DisabledException.class);
        }
        catch (DataKeyGenerationException ex) {
            assertEquals("Failed to generate a new data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyGenerationException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesDependencyTimeoutException() {
        try {
            throwException(DependencyTimeoutException.class);
        }
        catch (DataKeyGenerationException ex) {
            assertEquals("Failed to generate a new data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyGenerationException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesInvalidKeyUsageException() {
        try {
            throwException(InvalidKeyUsageException.class);
        }
        catch (DataKeyGenerationException ex) {
            assertEquals("Failed to generate a new data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyGenerationException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesInvalidGrantTokenException () {
        try {
            throwException(InvalidGrantTokenException.class);
        }
        catch (DataKeyGenerationException ex) {
            assertEquals("Failed to generate a new data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyGenerationException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesKMSInternalException() {
        try {
            throwException(KMSInternalException.class);
        }
        catch (DataKeyGenerationException ex) {
            assertEquals("Failed to generate a new data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyGenerationException.class + " got " + e.getClass() + ".");
        }
    }

    @Test
    public void handlesKMSInvalidStateException() {
        try {
            throwException(KMSInvalidStateException.class);
        }
        catch (DataKeyGenerationException ex) {
            assertEquals("Failed to generate a new data key due to an internal error. Try again later.", ex.getMessage());
        }
        catch (Exception  e) {
            fail("Expected " + DataKeyGenerationException.class + " got " + e.getClass() + ".");
        }
    }

    private void throwException(Class<? extends Exception> e) throws CurrentKeyIdException {
        given(awsKms.generateDataKey(ArgumentMatchers.any(GenerateDataKeyRequest.class)))
                .willThrow(e);
        dataKeyGeneratorProvider.generateDataKey("DATAKEYENCRYPTIONKEYID");
    }

    @Autowired
    private DataKeyGeneratorProvider dataKeyGeneratorProvider;

    @Autowired
    private AWSKMS awsKms;
}