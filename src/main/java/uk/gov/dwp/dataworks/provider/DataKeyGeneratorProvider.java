package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;

public interface DataKeyGeneratorProvider extends Dependent {

    GenerateDataKeyResponse generateDataKey(String encryptionKeyId, String correlationId)
            throws DataKeyGenerationException, MasterKeystoreException;

    @Override
    boolean canSeeDependencies();
}
