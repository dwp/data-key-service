package uk.gov.dwp.dataworks.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.service.DataKeyService;
import uk.gov.dwp.dataworks.util.CertificateUtils;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"server.environment_name=development",
        "cache.eviction.interval=1000",
        "key.cache.eviction.interval=1000",
        "decrypted.key.cache.eviction.interval=1000",
        "scheduling.enabled=false",
        "server.environment_name=test"
})
public class DataKeyServiceTest {

    private final String correlationId = "correlation";

    @Before
    public void init() {
        Mockito.reset(currentKeyIdProvider);

        for (String name : cacheManager.getCacheNames()) {
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        }
    }

    @Test
    public void Should_Verify_Cache_Returns_Key_Id_When_Invoked_With_In_Cache_Eviction_Interval() {
        dataKeyService.currentKeyId(correlationId);
        dataKeyService.currentKeyId(correlationId);
        dataKeyService.currentKeyId(correlationId);

        Mockito.verify(currentKeyIdProvider, Mockito.times(1)).getKeyId(correlationId);
    }

    @Test
    public void Should_Verify_Cache_Evicts_At_Specified_Interval() throws InterruptedException {
        dataKeyService.currentKeyId(correlationId);
        Thread.sleep(2000);
        dataKeyService.currentKeyId(correlationId);

        Mockito.verify(currentKeyIdProvider, Mockito.times(2)).getKeyId(correlationId);
    }

    @Test
    public void cachesDecryptedKeys() throws MasterKeystoreException {
        DecryptDataKeyResponse expected =
                new DecryptDataKeyResponse("dataKeyDecryptionKeyId", "plaintextDataKey");
        Mockito.when(dataKeyDecryptionProvider.decryptDataKey("keyId", "ciphertextDataKey", "correlationId"))
                .thenReturn(expected);
        DecryptDataKeyResponse firstActual = dataKeyService.decrypt("keyId", "ciphertextDataKey", "correlationId");
        DecryptDataKeyResponse secondActual = dataKeyService.decrypt("keyId", "ciphertextDataKey", "correlationId");
        assertEquals(expected, firstActual);
        assertEquals(expected, secondActual);
        Mockito.verify(dataKeyDecryptionProvider, Mockito.times(1)).decryptDataKey("keyId", "ciphertextDataKey", "correlationId");
        Mockito.verifyNoMoreInteractions(dataKeyDecryptionProvider);
    }

    @Test
    public void evictsDecryptedKeyCache() throws MasterKeystoreException, InterruptedException {
        DecryptDataKeyResponse expected =
                new DecryptDataKeyResponse("dataKeyDecryptionKeyId", "plaintextDataKey");
        Mockito.when(dataKeyDecryptionProvider.decryptDataKey("keyId", "ciphertextDataKey", "correlationId"))
                .thenReturn(expected);
        DecryptDataKeyResponse firstActual = dataKeyService.decrypt("keyId", "ciphertextDataKey", "correlationId");
        Thread.sleep(2_000);
        DecryptDataKeyResponse secondActual = dataKeyService.decrypt("keyId", "ciphertextDataKey", "correlationId");
        assertEquals(expected, firstActual);
        assertEquals(expected, secondActual);
        Mockito.verify(dataKeyDecryptionProvider, Mockito.times(2)).decryptDataKey("keyId", "ciphertextDataKey", "correlationId");
        Mockito.verifyNoMoreInteractions(dataKeyDecryptionProvider);
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private DataKeyService dataKeyService;

    @MockBean
    private DataKeyGeneratorProvider dataKeyGeneratorProvider;

    @MockBean
    private CurrentKeyIdProvider currentKeyIdProvider;

    @MockBean
    private DataKeyDecryptionProvider dataKeyDecryptionProvider;

    @MockBean
    private CertificateUtils certificateUtils;
}
