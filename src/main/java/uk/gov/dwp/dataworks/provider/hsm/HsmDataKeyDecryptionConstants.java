package uk.gov.dwp.dataworks.provider.hsm;

import java.util.regex.Pattern;

public interface HsmDataKeyDecryptionConstants {
    boolean EXTRACTABLE = true;
    boolean NOT_PERSISTENT = false;

    String PADDING = "OAEPPadding";
    String SYMMETRIC_KEY_TYPE = "AES";
    String DATA_KEY_LABEL = "data_key";
    String CAVIUM_PROVIDER = "Cavium";

    String PRIVATE_KEY_GROUP_NAME = "privateKeyHandle";
    String PUBLIC_KEY_GROUP_NAME = "publicKeyHandle";

    Pattern KEY_ID_PATTERN = Pattern.compile("^cloudhsm:(?<" + PRIVATE_KEY_GROUP_NAME + ">\\d+)[/,](?<" + PUBLIC_KEY_GROUP_NAME + ">\\d+)$");

    // Will retry at 1s, 2s, 4s, 8, 16 then give up (after a total of 31 secs)
    int MAX_ATTEMPTS = 5;
    int INITIAL_BACKOFF_MILLIS = 1000;
    double BACKOFF_MULTIPLIER = 2.0;
}
