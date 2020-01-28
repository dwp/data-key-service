package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;

public interface CurrentKeyIdProvider extends Dependent {
    String getKeyId(String dksCorrelationId) throws CurrentKeyIdException;
}
