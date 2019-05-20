package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;

public interface CurrentKeyIdProvider {
    String getKeyId() throws CurrentKeyIdException;
}
