package uk.gov.dwp.dataworks.provider.hsm;

import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.provider.Dependent;
import uk.gov.dwp.dataworks.provider.HsmLoginManager;

import java.util.regex.Matcher;

public abstract class HsmDependent implements Dependent, HsmDataKeyDecryptionConstants {

    public HsmDependent(HsmLoginManager loginManager) {
        this.loginManager = loginManager;
    }

    int privateKeyHandle(String keyId, String correlationId) {
        return keyHandle(keyId, PRIVATE_KEY_GROUP_NAME, correlationId);
    }

    int publicKeyHandle(String keyId, String correlationId) {
        return keyHandle(keyId, PUBLIC_KEY_GROUP_NAME, correlationId);
    }

    private int keyHandle(String keyId, String groupName, String correlationId) {
        Matcher matcher = KEY_ID_PATTERN.matcher(keyId);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(groupName));
        } else {
            throw new CurrentKeyIdException(correlationId);
        }
    }

    @Override
    public boolean canSeeDependencies() {
        return this.loginManager != null;
    }

    final HsmLoginManager loginManager;
}
