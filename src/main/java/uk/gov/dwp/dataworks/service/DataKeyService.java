package uk.gov.dwp.dataworks.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.LoginException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.logging.DataworksLogger;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

@Service
public class DataKeyService {
    private final DataKeyGeneratorProvider dataKeyProvider;
    private final CurrentKeyIdProvider currentKeyIdProvider;
    private final DataKeyDecryptionProvider dataKeyDecryptionProvider;
    private static final String KEY_CACHE = "keycache";
    private static final String DECRYPTED_CACHE = "decrypted_cache";
    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(DataKeyService.class.toString());


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
    public String currentKeyId(String correlationId) {
        return currentKeyIdProvider.getKeyId(correlationId);
    }

    @Scheduled(fixedRateString = "${key.cache.eviction.interval:120000}")
    @CacheEvict(value = KEY_CACHE, allEntries = true)
    public void clearCache() {
        LOGGER.debug("Key cache evicted.");
    }

    public GenerateDataKeyResponse generate(String keyId, String correlationId) throws LoginException, MasterKeystoreException {
        return dataKeyProvider.generateDataKey(keyId, correlationId);
    }

    @Scheduled(fixedRateString = "${decrypted.key.cache.eviction.interval:86400000}") // daily
    @CacheEvict(value = DECRYPTED_CACHE, allEntries = true)
    public void clearDecryptedCache() {
        LOGGER.info("Decrypted key cache evicted.");
    }

    @Cacheable(DECRYPTED_CACHE)
    public DecryptDataKeyResponse decrypt(String dataKeyId, String ciphertextDataKey, String correlationId)
            throws LoginException, MasterKeystoreException {
        return dataKeyDecryptionProvider.decryptDataKey(dataKeyId, ciphertextDataKey, correlationId);
    }

    public boolean canSeeDependencies() {
        return dataKeyProvider != null && dataKeyProvider.canSeeDependencies() &&
                currentKeyIdProvider != null && currentKeyIdProvider.canSeeDependencies() &&
                dataKeyDecryptionProvider != null && dataKeyDecryptionProvider.canSeeDependencies();
    }
}
