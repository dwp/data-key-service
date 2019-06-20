package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;

public interface DataKeyGeneratorProvider extends Dependent {
    GenerateDataKeyResponse generateDataKey(String keyId) throws DataKeyGenerationException;
    boolean canSeeDependencies();
}
