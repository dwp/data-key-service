package uk.gov.dwp.dataworks.provider;

import uk.gov.dwp.dataworks.errors.MasterKeystoreException;

public interface HsmLoginManager {
    void login() throws MasterKeystoreException;
    void logout() throws MasterKeystoreException;
}
