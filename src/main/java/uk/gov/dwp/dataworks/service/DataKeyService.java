package uk.gov.dwp.dataworks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

@Service
public class DataKeyService {
    private final DataKeyGeneratorProvider dataKeyProvider;
    private final CurrentKeyIdProvider currentKeyIdProvider;
    private final DataKeyDecryptionProvider dataKeyDecryptionProvider;
    private static final String KEY_CACHE = "keycache";
    private final static Logger LOGGER = LoggerFactory.getLogger(DataKeyService.class);

    @Autowired
    public DataKeyService(
            DataKeyGeneratorProvider dataKeyProvider,
            CurrentKeyIdProvider currentKeyIdProvider,
            DataKeyDecryptionProvider dataKeyDecryptionProvider) {
        this.dataKeyProvider = dataKeyProvider;
        this.currentKeyIdProvider = currentKeyIdProvider;
        this.dataKeyDecryptionProvider = dataKeyDecryptionProvider;
    }

    @Cacheable(value = KEY_CACHE, key = "#root.methodName")
    public String currentKeyId() {
        return currentKeyIdProvider.getKeyId();
    }

    @Scheduled(fixedRateString = "${key.cache.eviction.interval:120000}")
    @CacheEvict(value = KEY_CACHE, allEntries = true)
    public void clearCache() {
        LOGGER.info("Cache evicted");

    }

    public GenerateDataKeyResponse generate(String keyId) {

        return dataKeyProvider.generateDataKey(keyId);
    }

    public DecryptDataKeyResponse decrypt(String dataKeyId, String ciphertextDataKey) {
        return dataKeyDecryptionProvider.decryptDataKey(dataKeyId, ciphertextDataKey);
    }

    public boolean canSeeDependencies() {
        return dataKeyProvider != null && dataKeyProvider.canSeeDependencies() &&
                currentKeyIdProvider != null && currentKeyIdProvider.canSeeDependencies() &&
                dataKeyDecryptionProvider != null && dataKeyDecryptionProvider.canSeeDependencies();
    }
}
