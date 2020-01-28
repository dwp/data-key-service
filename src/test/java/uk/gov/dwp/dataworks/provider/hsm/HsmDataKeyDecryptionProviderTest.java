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
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles({"UnitTest", "HSM"})
@TestPropertySource(properties = {"server.environment_name=development",
        "cache.eviction.interval=1000",
        "scheduling.enabled=false"
})
public class HsmDataKeyDecryptionProviderTest {

    private final String correlationId = "correlation";

    @Before
    public void init() {
        Mockito.reset(cryptoImplementationSupplier);
        Mockito.reset(hsmLoginManager);
    }

    @Test
    public void decryptDataKey_happy_case() throws CryptoImplementationSupplierException, MasterKeystoreException {
        Integer privateKeyHandle = 1;
        int publicKeyHandle = 2;
        String encryptedDataKey = "ENCRYPTED_DATA_KEY";
        String plaintextDataKey = "PLAINTEXT_DATA_KEY";
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        given(cryptoImplementationSupplier.decryptedKey(privateKeyHandle, encryptedDataKey, correlationId)).willReturn(plaintextDataKey);

        DecryptDataKeyResponse actual = dataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, encryptedDataKey, correlationId);

        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(cryptoImplementationSupplier, times(1))
                .decryptedKey(argumentCaptor.capture(), same(encryptedDataKey), eq(correlationId));
        DecryptDataKeyResponse expected = new DecryptDataKeyResponse(dataKeyEncryptionKeyId, plaintextDataKey);
        assertEquals(actual, expected);
    }

    @Test
    public void decryptDataKeyNotOk() throws CryptoImplementationSupplierException, MasterKeystoreException {
        Integer privateKeyHandle = 1;
        int publicKeyHandle = 2;
        String encryptedDataKey = "ENCRYPTED_DATA_KEY";
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        given(cryptoImplementationSupplier.decryptedKey(privateKeyHandle, encryptedDataKey, correlationId))
                .willThrow(CryptoImplementationSupplierException.class);

        try {
            dataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, encryptedDataKey, correlationId);
            fail("Expected a DataKeyDecryptionException");
        } catch (DataKeyDecryptionException ex) {
            assertEquals("Failed to decrypt this data key due to an internal error. Try again later. correlation_id: correlation", ex.getMessage());
        }
    }

    @Test
    public void malformedMasterKeyId() throws MasterKeystoreException {
        String dataKeyEncryptionKeyId = "cloudhsm:NOT_IN_CORRECT_FORMAT";
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();

        try {
            dataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, "ENCRYPTED", correlationId);
            fail("Expected a CurrentKeyIdException");
        } catch (CurrentKeyIdException ex) {
            assertEquals("Failed to retrieve the current key id. correlation_id: correlation", ex.getMessage());
        }
    }

    @Autowired
    private DataKeyDecryptionProvider dataKeyDecryptionProvider;

    @Autowired
    private CryptoImplementationSupplier cryptoImplementationSupplier;

    @MockBean
    private ImplicitHsmLoginManager hsmLoginManager;
}
