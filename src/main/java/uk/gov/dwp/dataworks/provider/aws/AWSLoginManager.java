package uk.gov.dwp.dataworks.provider.aws;

import uk.gov.dwp.dataworks.errors.LoginException;

public interface AWSLoginManager {

    void login() throws LoginException;
    void logout() throws LoginException;
}
