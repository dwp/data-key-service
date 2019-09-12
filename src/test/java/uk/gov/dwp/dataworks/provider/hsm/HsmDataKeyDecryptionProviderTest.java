package uk.gov.dwp.dataworks.provider.hsm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionException;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles({"UnitTest", "HSM"})
@TestPropertySource(properties = {"server.environment_name=development",
        "cache.eviction.interval=1000"
})
public class HsmDataKeyDecryptionProviderTest {

    @Before
    public void init() {
        Mockito.reset(cryptoImplementationSupplier);
        Mockito.reset(hsmLoginManager);
    }

    @Test
    public void decryptDataKey() throws CryptoImplementationSupplierException {
        Integer privateKeyHandle = 1;
        int publicKeyHandle = 2;
        String encryptedDataKey = "ENCRYPTED_DATA_KEY";
        String plaintextDataKey = "PLAINTEXT_DATA_KEY";
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
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
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        given(cryptoImplementationSupplier.decryptedKey(privateKeyHandle, encryptedDataKey)).willThrow(CryptoImplementationSupplierException.class);
        dataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, encryptedDataKey);
    }

    @Test(expected = CurrentKeyIdException.class)
    public void malformedMasterKeyId() {
        String dataKeyEncryptionKeyId = "cloudhsm:NOT_IN_CORRECT_FORMAT";
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
        dataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, "ENCRYPTED");
    }

    @Autowired
    private DataKeyDecryptionProvider dataKeyDecryptionProvider;

    @Autowired
    private CryptoImplementationSupplier cryptoImplementationSupplier;

    @MockBean
    private HsmLoginManager hsmLoginManager;
}