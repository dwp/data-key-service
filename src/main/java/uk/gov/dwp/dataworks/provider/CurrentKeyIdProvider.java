package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.errors.CurrentKeyIdFailure;

public interface CurrentKeyIdProvider {
    String getKeyId() throws CurrentKeyIdFailure;
}
