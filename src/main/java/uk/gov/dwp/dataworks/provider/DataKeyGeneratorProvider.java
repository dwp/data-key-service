package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;

public interface DataKeyGeneratorProvider {
    GenerateDataKeyResponse generateDataKey(String keyId);
}
