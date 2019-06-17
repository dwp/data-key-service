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

    @Test(expected = GarbledDataKeyException.class)
    public void handlesInvalidCiphertextException() {
        throwException(InvalidCiphertextException.class);
    }

    @Test(expected = GarbledDataKeyException.class)
    public void handlesIllegalArgumentException() {
        throwException(IllegalArgumentException.class);
    }

    @Test(expected = DataKeyDecryptionException.class)
    public void handlesNotFoundException() {
        throwException(NotFoundException.class);
    }

    @Test(expected = DataKeyDecryptionException.class)
    public void handlesDisabledException() {
        throwException(DisabledException.class);
    }

    @Test(expected = DataKeyDecryptionException.class)
    public void handlesKeyUnavailableException() {
        throwException(KeyUnavailableException.class);
    }

    @Test(expected = DataKeyDecryptionException.class)
    public void handlesDependencyTimeoutException() {
        throwException(DependencyTimeoutException.class);
    }

    @Test(expected = DataKeyDecryptionException.class)
    public void handlesInvalidGrantTokenException() {
        throwException(InvalidGrantTokenException.class);
    }

    @Test(expected = DataKeyDecryptionException.class)
    public void handlesKMSInternalException() {
        throwException(KMSInternalException.class);
    }

    @Test(expected = DataKeyDecryptionException.class)
    public void handlesKMSInvalidStateException() {
        throwException(KMSInvalidStateException.class);
    }

    @Test(expected = DataKeyDecryptionException.class)
    public void handlesRuntimeException() {
        throwException(RuntimeException.class);
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