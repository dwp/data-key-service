package hsm.spike;

import com.cavium.cfm2.CFM2Exception;
import com.cavium.cfm2.Util;
import com.cavium.key.CaviumKey;
import com.cavium.key.CaviumKeyAttributes;
import com.cavium.key.CaviumRSAPrivateKey;
import com.cavium.key.CaviumRSAPublicKey;
import com.cavium.key.parameter.CaviumAESKeyGenParameterSpec;
import com.cavium.key.parameter.CaviumKeyGenAlgorithmParameterSpec;
import com.cavium.provider.CaviumProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/*
 * Before working on this code you must install the aws hsm sdk and then install the
 * client jar into your local maven repository.
 *
 * To install the sdk:
 * {@link https://docs.aws.amazon.com/cloudhsm/latest/userguide/java-library-install.html}
 *
 * To put the jar into your maven repository:
 * {@code mvn install:install-file -DgroupId=cloudhsm -DartifactId=cloudhsm -Dversion=2.0.4 -Dfile=./cloudhsm-2.0.4.jar -Dpackaging=jar}
 *
 * In CloudHSM the master rsa key pair should be created as non-exportable. This results in an
 * exportable public key, and a non-exportable private key. To do this issue the command
 * {@code genRSAKeyPair -m 2048 -e 65537 -l MASTER_KEY_PAIR -nex}
 * in the key management util.
 *
 * Before running the login credentials need to be made available as system properties
 * or as environment variables. In either case the properties are
 * HSM_PARTITION
 * HSM_USER
 * HSM_PASSWORD
 *
 * To compile on an instance with the hsm client daemon:
 * {@code javac -cp "/opt/cloudhsm/java/*" -d hsm/spike Dks.java}
 *
 * To run:
 * {@code java -Djava.library.path=/opt/cloudhsm/lib -cp "/opt/cloudhsm/java/*:." hsm.spike.Dks}
 */
public class Dks {


    public static void main(String[] args) throws Exception {
        Security.addProvider(new CaviumProvider());
        Map<String, String> newDataKey = dataKey();
        String plaintext = newDataKey.get(PLAINTEXT_HASH_KEY);
        String ciphertext = newDataKey.get(CIPHERTEXT_HASH_KEY);
        String decrypted = decryptDataKey(ciphertext);

        System.out.println("ciphertext:            '" + ciphertext + "'");
        System.out.println("plaintextDataKey:      '" + plaintext + "'");
        System.out.println("plaintextUnwrappedKey: '" + decrypted + "'");
    }


    private final static Map<String, String> dataKey() throws Exception {
        Security.addProvider(new CaviumProvider());
        KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_KEY_TYPE, PROVIDER);
        CaviumAESKeyGenParameterSpec aesSpec =
                new CaviumAESKeyGenParameterSpec(128, DATA_KEY_LABEL, EXTRACTABLE, NOT_PERSISTENT);
        keyGenerator.init(aesSpec);
        CaviumKey dataKey = (CaviumKey) keyGenerator.generateKey();

        byte[] plaintextDatakey = Base64.getEncoder().encode(dataKey.getEncoded());

        byte[] keyAttribute = Util.getKeyAttributes(PUBLIC_KEY_HANDLE);
        CaviumRSAPublicKey publicKey = new CaviumRSAPublicKey(PUBLIC_KEY_HANDLE,  new CaviumKeyAttributes(keyAttribute));

        if (publicKey.getEncoded() == null) {
            throw new IllegalStateException("Non exportable public key!");
        }
        byte[] wrappedKey = Util.rsaWrapKey(publicKey, dataKey, PADDING);
        byte[] ciphertext = Base64.getEncoder().encode(wrappedKey);

        Map<String, String> payload = new HashMap<>();
        payload.put(PLAINTEXT_HASH_KEY, new String(plaintextDatakey));
        payload.put(CIPHERTEXT_HASH_KEY, new String(ciphertext));

        return payload;
    }

    private static String decryptDataKey(String ciphertext)
            throws CFM2Exception, InvalidKeyException, NoSuchAlgorithmException {
        byte[] decodedCipher = Base64.getDecoder().decode(ciphertext.getBytes());
        byte[] privateKeyAttribute = Util.getKeyAttributes(PRIVATE_KEY_HANDLE);
        CaviumKeyAttributes privateAttributes = new CaviumKeyAttributes(privateKeyAttribute);
        CaviumRSAPrivateKey privateKey = new CaviumRSAPrivateKey(PRIVATE_KEY_HANDLE, privateAttributes);

        if (privateKey.getEncoded() != null) {
            throw new IllegalStateException("Private key has escaped the HSM!");
        }

        CaviumKeyGenAlgorithmParameterSpec unwrappingSpec = new
                CaviumKeyGenAlgorithmParameterSpec(DATA_KEY_LABEL, EXTRACTABLE, NOT_PERSISTENT);

        CaviumKey unwrappedKey =
                Util.rsaUnwrapKey(privateKey, decodedCipher, SYMMETRIC_KEY_TYPE, Cipher.SECRET_KEY, unwrappingSpec, PADDING);
        byte[] exportedUnwrappedKey = unwrappedKey.getEncoded();
        byte[] plaintextUnwrappedKey = Base64.getEncoder().encode(exportedUnwrappedKey);
        return new String(plaintextUnwrappedKey);
    }

    public static final String PROVIDER = "Cavium";

    private static final String PLAINTEXT_HASH_KEY = "plaintext";
    private static final String CIPHERTEXT_HASH_KEY = "ciphertext";

    private static final int PRIVATE_KEY_HANDLE = 7;
    private static final int PUBLIC_KEY_HANDLE = 14;

    private static final String PADDING = "OAEPPadding";
    private static final String SYMMETRIC_KEY_TYPE = "AES";
    private static final String DATA_KEY_LABEL = "data_key";

    public static final boolean EXTRACTABLE = true;
    public static final boolean NOT_PERSISTENT = false;
}
