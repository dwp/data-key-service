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
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.service.DataKeyService;

import java.util.Objects;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"server.environment_name=development",
        "cache.eviction.interval=1000",
        "key.cache.eviction.interval=1000",
        "scheduling.enabled=false"
})
public class DataKeyServiceTest {

    @Before
    public void init() {
        Mockito.reset(currentKeyIdProvider);

        for (String name : cacheManager.getCacheNames()) {
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        }
    }

    @Test
    public void Should_Verify_Cache_Returns_Key_Id_When_Invoked_With_In_Cache_Eviction_Interval() {
        dataKeyService.currentKeyId();
        dataKeyService.currentKeyId();
        dataKeyService.currentKeyId();

        Mockito.verify(currentKeyIdProvider, Mockito.times(1)).getKeyId();
    }

    @Test
    public void Should_Verify_Cache_Evicts_At_Specified_Interval() throws InterruptedException {
        dataKeyService.currentKeyId();
        Thread.sleep(2000);
        dataKeyService.currentKeyId();

        Mockito.verify(currentKeyIdProvider, Mockito.times(2)).getKeyId();
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
}
