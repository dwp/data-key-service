package uk.gov.dwp.dataworks.provider.hsm;

import com.cavium.key.CaviumKey;
import com.cavium.key.parameter.CaviumAESKeyGenParameterSpec;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.provider.Dependent;

import javax.crypto.KeyGenerator;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.regex.Matcher;

public class HsmDependent implements Dependent, HsmDataKeyDecryptionConstants {

    @Autowired
    HSMLoginManager loginManager;

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
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_KEY_TYPE, PROVIDER);
            CaviumAESKeyGenParameterSpec aesSpec =
                    new CaviumAESKeyGenParameterSpec(128, DATA_KEY_LABEL, EXTRACTABLE, NOT_PERSISTENT);
            keyGenerator.init(aesSpec);
            CaviumKey dataKey = (CaviumKey) keyGenerator.generateKey();
            return dataKey != null;
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            return false;
        }
    }
}
