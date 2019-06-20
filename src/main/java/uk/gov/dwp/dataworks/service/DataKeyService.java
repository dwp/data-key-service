package uk.gov.dwp.dataworks.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public DataKeyService(
            DataKeyGeneratorProvider dataKeyProvider,
            CurrentKeyIdProvider currentKeyIdProvider,
            DataKeyDecryptionProvider dataKeyDecryptionProvider) {
        this.dataKeyProvider = dataKeyProvider;
        this.currentKeyIdProvider = currentKeyIdProvider;
        this.dataKeyDecryptionProvider = dataKeyDecryptionProvider;
    }

    public String currentKeyId() {
        return currentKeyIdProvider.getKeyId();
    }

    public GenerateDataKeyResponse generate() {
        return dataKeyProvider.generateDataKey(currentKeyId());
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
