package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.key.CaviumKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.dwp.dataworks.provider.hsm.HsmDataKeyDecryptionConstants.MAX_ATTEMPTS;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles({"UnitTest", "HSM"})
@TestPropertySource(properties = {"server.environment_name=development",
        "cache.eviction.interval=1000",
        "scheduling.enabled=false"
})
public class HsmDataKeyGeneratorProviderTest {

    private final String correlationId = "correlation";

    @Before
    public void init() {
        Mockito.reset(cryptoImplementationSupplier);
        Mockito.reset(hsmLoginManager);
    }

    @Test
    public void generateDataKey() throws CryptoImplementationSupplierException, MasterKeystoreException {
        int privateKeyHandle = 1;
        int publicKeyHandle = 2;
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        String plainTextKey = "PLAINTEXTKEY";
        String encryptedDataKey = "ENCRYPTEDDATAKEY";
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
        CaviumKey key = Mockito.mock(CaviumKey.class);
        given(key.getEncoded()).willReturn(plainTextKey.getBytes());
        given(cryptoImplementationSupplier.dataKey(correlationId)).willReturn(key);
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        given(cryptoImplementationSupplier.encryptedKey(publicKeyHandle, key, correlationId)).willReturn(encryptedDataKey.getBytes());

        GenerateDataKeyResponse actual = dataKeyGeneratorProvider.generateDataKey(dataKeyEncryptionKeyId, correlationId);

        verify(cryptoImplementationSupplier, times(1)).encryptedKey(argumentCaptor.capture(), same(key), eq(correlationId));
        assertEquals(argumentCaptor.getValue(), Integer.valueOf(publicKeyHandle));
        GenerateDataKeyResponse expected = new GenerateDataKeyResponse(dataKeyEncryptionKeyId, Base64
                .getEncoder().encodeToString(plainTextKey.getBytes()), encryptedDataKey);
        assertEquals(actual, expected);
    }

    @Test
    public void generateDataKey_will_retry_until_maximum_reached() throws MasterKeystoreException, CryptoImplementationSupplierException {
        try {

            CaviumKey mockKey = Mockito.mock(CaviumKey.class);
            given(mockKey.getEncoded()).willReturn("some bytes".getBytes());
            given(cryptoImplementationSupplier
                    .dataKey(eq(correlationId)))
                    .willReturn(mockKey);
            given(cryptoImplementationSupplier
                    .encryptedKey(any(), any(), eq(correlationId)))
                    .willThrow(new MasterKeystoreException("Boom"));

            dataKeyGeneratorProvider.generateDataKey("cloudhsm:1,2", correlationId);
            fail("Expected a MasterKeystoreException");
        } catch (MasterKeystoreException ex) {
            assertEquals("Boom", ex.getMessage());
            verify(cryptoImplementationSupplier, times(MAX_ATTEMPTS)).encryptedKey(any(), any(), eq(correlationId));
        }
    }

    @Test
    public void generateDataKeyNotOk() throws CryptoImplementationSupplierException, MasterKeystoreException {
        int privateKeyHandle = 1;
        int publicKeyHandle = 2;
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        String plainTextKey = "PLAINTEXTKEY";
        CaviumKey key = Mockito.mock(CaviumKey.class);
        given(key.getEncoded()).willReturn(plainTextKey.getBytes());
        given(cryptoImplementationSupplier.dataKey(correlationId)).willThrow(CryptoImplementationSupplierException.class);

        try {
            dataKeyGeneratorProvider.generateDataKey(dataKeyEncryptionKeyId, correlationId);
            fail("Expected a DataKeyGenerationException");
        } catch (DataKeyGenerationException ex){
            assertEquals("Failed to generate a new data key due to an internal error. Try again later. correlation_id: correlation", ex.getMessage());
        }
    }

    @Test
    public void encryptDataKeyNotOk() throws CryptoImplementationSupplierException, MasterKeystoreException {
        int privateKeyHandle = 1;
        Integer publicKeyHandle = 2;
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        String plainTextKey = "PLAINTEXTKEY";
        CaviumKey key = Mockito.mock(CaviumKey.class);
        given(key.getEncoded()).willReturn(plainTextKey.getBytes());
        given(cryptoImplementationSupplier.dataKey(correlationId)).willReturn(key);
        given(cryptoImplementationSupplier.encryptedKey(publicKeyHandle, key, correlationId)).willThrow(CryptoImplementationSupplierException.class);

        try {
            dataKeyGeneratorProvider.generateDataKey(dataKeyEncryptionKeyId, correlationId);
            fail("Expected a DataKeyGenerationException");
        } catch (DataKeyGenerationException ex){
            assertEquals("Failed to generate a new data key due to an internal error. Try again later. correlation_id: correlation", ex.getMessage());
        }
    }

    @Test
    public void testBadMasterKeyId() throws MasterKeystoreException {
        String dataKeyEncryptionKeyId = "NOT IN THE CORRECT FORMAT";
        try {
            dataKeyGeneratorProvider.generateDataKey(dataKeyEncryptionKeyId, correlationId);
            fail("Expected a DataKeyGenerationException");
        } catch (CurrentKeyIdException ex){
            assertEquals("Failed to retrieve the current key id. correlation_id: correlation", ex.getMessage());
        }
    }

    @Autowired
    private DataKeyGeneratorProvider dataKeyGeneratorProvider;

    @Autowired
    private CryptoImplementationSupplier cryptoImplementationSupplier;

    @MockBean
    private ImplicitHsmLoginManager hsmLoginManager;

}
