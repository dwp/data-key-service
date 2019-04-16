package uk.gov.dwp.dataworks.dto;

import io.swagger.annotations.ApiModelProperty;

@SuppressWarnings({"unused", "WeakerAccess"})
public class DecryptDataKeyResponse {

    @ApiModelProperty(notes="The id of the key used to encrypt/decrypt this data key's ciphertext")
    public String dataKeyEncryptionKeyId;

    @ApiModelProperty(notes="The securely generated Initialisation Vector you need to use to decrypt data when using this data key")
    public String iv;

    @ApiModelProperty(notes="The decrypted data key")
    public String plaintextDataKey;

    public DecryptDataKeyResponse() {
    }

    public DecryptDataKeyResponse(String dataKeyEncryptionKeyId, String iv, String plaintextDataKey) {
        this.dataKeyEncryptionKeyId = dataKeyEncryptionKeyId;
        this.iv = iv;
        this.plaintextDataKey = plaintextDataKey;
    }
}
