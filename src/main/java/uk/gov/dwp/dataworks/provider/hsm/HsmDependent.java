package uk.gov.dwp.dataworks.provider.hsm;

import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.errors.LoginException;
import uk.gov.dwp.dataworks.provider.Dependent;
import uk.gov.dwp.dataworks.provider.aws.AWSLoginManager;

import java.util.regex.Matcher;

public class HsmDependent implements Dependent, HsmDataKeyDecryptionConstants {

    public HsmDependent(AWSLoginManager loginManager) {
        this.loginManager = loginManager;
    }

    int privateKeyHandle(String keyId) {
        return keyHandle(keyId, PRIVATE_KEY_GROUP_NAME);
    }

    int publicKeyHandle(String keyId) {
        return keyHandle(keyId, PUBLIC_KEY_GROUP_NAME);
    }

    private int keyHandle(String keyId, String groupName) {
        Matcher matcher = KEY_ID_PATTERN.matcher(keyId);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(groupName));
        }
        else {
            throw new CurrentKeyIdException();
        }
    }

    @Override
    public boolean canSeeDependencies() {
        try {
            loginManager.login();
            loginManager.logout();
            return true;
        }
        catch (LoginException e) {
            return false;
        }
    }

    protected AWSLoginManager loginManager;
}
