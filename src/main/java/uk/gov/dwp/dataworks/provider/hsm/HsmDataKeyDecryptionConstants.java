package uk.gov.dwp.dataworks.provider.hsm;

import java.util.regex.Pattern;

public interface HsmDataKeyDecryptionConstants {
    String CAVIUM_PROVIDER = "Cavium";

    String PRIVATE_KEY_GROUP_NAME = "privateKeyHandle";
    String PUBLIC_KEY_GROUP_NAME = "publicKeyHandle";

    Pattern KEY_ID_PATTERN = Pattern.compile("^cloudhsm:(?<" + PRIVATE_KEY_GROUP_NAME + ">\\d+)[/,](?<" + PUBLIC_KEY_GROUP_NAME + ">\\d+)$");
}
