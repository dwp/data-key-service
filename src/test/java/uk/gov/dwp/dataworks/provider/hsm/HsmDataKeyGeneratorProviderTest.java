package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.key.CaviumKey;
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
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CryptoImplementationSupplierException;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles({"UnitTest", "HSM"})
@TestPropertySource(properties = {"server.environment_name=development",
        "cache.eviction.interval=1000"
})
public class HsmDataKeyGeneratorProviderTest {

    @Before
    public void init() {
        Mockito.reset(cryptoImplementationSupplier);
        Mockito.reset(hsmLoginManager);
    }

    @Test
    public void generateDataKey() throws CryptoImplementationSupplierException {
        int privateKeyHandle = 1;
        int publicKeyHandle = 2;
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        String plainTextKey = "PLAINTEXTKEY";
        String encryptedDataKey = "ENCRYPTEDDATAKEY";
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
        CaviumKey key = Mockito.mock(CaviumKey.class);
        given(key.getEncoded()).willReturn(plainTextKey.getBytes());
        given(cryptoImplementationSupplier.dataKey()).willReturn(key);
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        given(cryptoImplementationSupplier.encryptedKey(publicKeyHandle, key)).willReturn(encryptedDataKey.getBytes());
        GenerateDataKeyResponse actual = dataKeyGeneratorProvider.generateDataKey(dataKeyEncryptionKeyId);
        verify(cryptoImplementationSupplier, times(1)).encryptedKey(argumentCaptor.capture(), same(key));
        assertEquals(argumentCaptor.getValue(), new Integer(publicKeyHandle));
        GenerateDataKeyResponse expected = new GenerateDataKeyResponse(dataKeyEncryptionKeyId, Base64
                .getEncoder().encodeToString(plainTextKey.getBytes()), encryptedDataKey);
        assertEquals(actual, expected);
    }

    @Test(expected = DataKeyGenerationException.class)
    public void generateDataKeyNotOk() throws CryptoImplementationSupplierException {
        int privateKeyHandle = 1;
        int publicKeyHandle = 2;
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        String plainTextKey = "PLAINTEXTKEY";
        CaviumKey key = Mockito.mock(CaviumKey.class);
        given(key.getEncoded()).willReturn(plainTextKey.getBytes());
        given(cryptoImplementationSupplier.dataKey()).willThrow(CryptoImplementationSupplierException.class);
        dataKeyGeneratorProvider.generateDataKey(dataKeyEncryptionKeyId);
    }

    @Test(expected = DataKeyGenerationException.class)
    public void encryptDataKeyNotOk() throws CryptoImplementationSupplierException {
        int privateKeyHandle = 1;
        Integer publicKeyHandle = 2;
        doNothing().when(hsmLoginManager).login();
        doNothing().when(hsmLoginManager).logout();
        String dataKeyEncryptionKeyId = "cloudhsm:" + privateKeyHandle + "/" + publicKeyHandle;
        String plainTextKey = "PLAINTEXTKEY";
        CaviumKey key = Mockito.mock(CaviumKey.class);
        given(key.getEncoded()).willReturn(plainTextKey.getBytes());
        given(cryptoImplementationSupplier.dataKey()).willReturn(key);
        given(cryptoImplementationSupplier.encryptedKey(publicKeyHandle, key)).willThrow(CryptoImplementationSupplierException.class);
        dataKeyGeneratorProvider.generateDataKey(dataKeyEncryptionKeyId);
    }

    @Test(expected = CurrentKeyIdException.class)
    public void testBadMasterKeyId() {
        String dataKeyEncryptionKeyId = "NOT IN THE CORRECT FORMAT";
        dataKeyGeneratorProvider.generateDataKey(dataKeyEncryptionKeyId);
    }

    @Autowired
    private DataKeyGeneratorProvider dataKeyGeneratorProvider;

    @Autowired
    private CryptoImplementationSupplier cryptoImplementationSupplier;

    @MockBean
    private HsmLoginManager hsmLoginManager;

}
