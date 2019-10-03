package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.errors.MasterKeystoreException;

public interface Dependent {
    boolean canSeeDependencies() throws MasterKeystoreException;
}
