package uk.gov.dwp.dataworks.provider.kms;

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
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

import java.nio.ByteBuffer;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles({ "UnitTest", "KMS" })
public class KMSDataKeyGeneratorProviderTest {

    private final String correlationId = "correlation";

    @Test
    public void generateDataKey() throws MasterKeystoreException {
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

        GenerateDataKeyResponse expected = new GenerateDataKeyResponse(dataKeyEncryptionKeyId,
                encoder.encodeToString(plainTextKey.getBytes()), encoder.encodeToString(encryptedDataKey.getBytes()));

        GenerateDataKeyResponse actual = dataKeyGeneratorProvider.generateDataKey(dataKeyEncryptionKeyId, correlationId);

        assertEquals(expected, actual);
    }

    @Test(expected = DataKeyGenerationException.class)
    public void handlesNotFoundException() throws MasterKeystoreException {
        throwException(NotFoundException.class);
    }

    @Test(expected = DataKeyGenerationException.class)
    public void handlesDisabledException() throws MasterKeystoreException {
        throwException(DisabledException.class);
    }

    @Test(expected = DataKeyGenerationException.class)
    public void handlesDependencyTimeoutException() throws MasterKeystoreException {
        throwException(DependencyTimeoutException.class);
    }

    @Test(expected = DataKeyGenerationException.class)
    public void handlesInvalidKeyUsageException() throws MasterKeystoreException {
        throwException(InvalidKeyUsageException.class);
    }

    @Test(expected = DataKeyGenerationException.class)
    public void handlesInvalidGrantTokenException() throws MasterKeystoreException {
        throwException(InvalidGrantTokenException.class);
    }

    @Test(expected = DataKeyGenerationException.class)
    public void handlesKMSInternalException() throws MasterKeystoreException {
        throwException(KMSInternalException.class);
    }

    @Test(expected = DataKeyGenerationException.class)
    public void handlesKMSInvalidStateException() throws MasterKeystoreException {
        throwException(KMSInvalidStateException.class);
    }

    private void throwException(Class<? extends Exception> e) throws CurrentKeyIdException, MasterKeystoreException {
        given(awsKms.generateDataKey(ArgumentMatchers.any(GenerateDataKeyRequest.class))).willThrow(e);
        dataKeyGeneratorProvider.generateDataKey("DATAKEYENCRYPTIONKEYID", correlationId);
    }

    @Autowired
    private DataKeyGeneratorProvider dataKeyGeneratorProvider;

    @Autowired
    private AWSKMS awsKms;
}
