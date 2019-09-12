package uk.gov.dwp.dataworks.dto;

import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

@SuppressWarnings({"unused", "WeakerAccess"})
public class DecryptDataKeyResponse {

    @ApiModelProperty(notes="The id of the key used to decrypt this data key's ciphertext")
    public final String dataKeyDecryptionKeyId;

    @ApiModelProperty(notes="The decrypted data key")
    public final String plaintextDataKey;

    public DecryptDataKeyResponse(String dataKeyDecryptionKeyId, String plaintextDataKey) {
        this.dataKeyDecryptionKeyId = dataKeyDecryptionKeyId;
        this.plaintextDataKey = plaintextDataKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DecryptDataKeyResponse that = (DecryptDataKeyResponse) o;
        return Objects.equals(dataKeyDecryptionKeyId, that.dataKeyDecryptionKeyId)
                && Objects.equals(plaintextDataKey, that.plaintextDataKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataKeyDecryptionKeyId, plaintextDataKey);
    }
}
