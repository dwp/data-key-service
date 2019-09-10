package uk.gov.dwp.dataworks.provider.hsm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionException;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles({"UnitTest", "HSM"})
public class HsmDataKeyDecryptionProviderTest {

    @Test
    public void decryptDataKey() throws CryptoImplementationSupplierException {
        Integer privateKeyHandle = 1;
        int publicKeyHandle = 2;
        String encryptedDataKey = "ENCRYPTED_DATA_KEY";
        String plaintextDataKey = "PLAINTEXT_DATA_KEY";
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        given(cryptoImplementationSupplier.decryptedKey(privateKeyHandle, encryptedDataKey)).willReturn(plaintextDataKey);
        DecryptDataKeyResponse actual = dataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, encryptedDataKey);
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(cryptoImplementationSupplier, times(1)).decryptedKey(argumentCaptor.capture(), same(encryptedDataKey));
        DecryptDataKeyResponse expected = new DecryptDataKeyResponse(dataKeyEncryptionKeyId, plaintextDataKey);
        assertEquals(actual, expected);
    }

    @Test(expected = DataKeyDecryptionException.class)
    public void decryptDataKeyNotOk() throws CryptoImplementationSupplierException {
        Integer privateKeyHandle = 1;
        int publicKeyHandle = 2;
        String encryptedDataKey = "ENCRYPTED_DATA_KEY";
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        given(cryptoImplementationSupplier.decryptedKey(privateKeyHandle, encryptedDataKey)).willThrow(CryptoImplementationSupplierException.class);
        dataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, encryptedDataKey);
    }

    @Test(expected = CurrentKeyIdException.class)
    public void malformedMasterKeyId() {
        String dataKeyEncryptionKeyId = "cloudhsm:NOT_IN_CORRECT_FORMAT";
        dataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, "ENCRYPTED");
    }

    @Autowired
    private DataKeyDecryptionProvider dataKeyDecryptionProvider;

    @Autowired
    private CryptoImplementationSupplier cryptoImplementationSupplier;
}