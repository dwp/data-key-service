package uk.gov.dwp.dataworks.dto;

import io.swagger.annotations.ApiModelProperty;

@SuppressWarnings({"unused", "WeakerAccess"})
public class GenerateDataKeyResponse {
    @ApiModelProperty(notes="The id of the encryption key that was used to encrypt the data key into ciphertext")
    public String dataKeyEncryptionKeyId;

    @ApiModelProperty(notes="The data key in plaintext")
    public String plaintextDataKey;

    @ApiModelProperty(notes="The data key in ciphertext, encrypted by the data key encryption key")
    public String ciphertextDataKey;

    public GenerateDataKeyResponse() {
    }

    public GenerateDataKeyResponse(String dataKeyEncryptionKeyId, String plaintextDataKey, String ciphertextDataKey) {
        this.dataKeyEncryptionKeyId = dataKeyEncryptionKeyId;
        this.plaintextDataKey = plaintextDataKey;
        this.ciphertextDataKey = ciphertextDataKey;
    }
}
