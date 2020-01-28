package uk.gov.dwp.dataworks.dto;

import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

@SuppressWarnings({"unused", "WeakerAccess"})
public class GenerateDataKeyResponse {
    @ApiModelProperty(notes = "The id of the encryption key that was used to encrypt the data key into ciphertext")
    public final String dataKeyEncryptionKeyId;

    @ApiModelProperty(notes = "The data key in plaintext")
    public final String plaintextDataKey;

    @ApiModelProperty(notes = "The data key in ciphertext, encrypted by the data key encryption key")
    public final String ciphertextDataKey;

    @ApiModelProperty(notes = "DKS Correlation Id passed in from the client call")
    public String correlationId = "NOT_SET";

    public GenerateDataKeyResponse(String dataKeyEncryptionKeyId, String plaintextDataKey, String ciphertextDataKey) {
        this.dataKeyEncryptionKeyId = dataKeyEncryptionKeyId;
        this.plaintextDataKey = plaintextDataKey;
        this.ciphertextDataKey = ciphertextDataKey;
    }

    public String getcorrelationId() {
        return correlationId;
    }

    public void setcorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public GenerateDataKeyResponse withcorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GenerateDataKeyResponse that = (GenerateDataKeyResponse) o;
        return Objects.equals(dataKeyEncryptionKeyId, that.dataKeyEncryptionKeyId)
                && Objects.equals(plaintextDataKey, that.plaintextDataKey)
                && Objects.equals(ciphertextDataKey, that.ciphertextDataKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataKeyEncryptionKeyId, plaintextDataKey, ciphertextDataKey);
    }

    @Override
    public String toString() {
        return "GenerateDataKeyResponse{" + "dataKeyEncryptionKeyId='" + dataKeyEncryptionKeyId + '\''
                + ", plaintextDataKey='" + plaintextDataKey + '\'' + ", ciphertextDataKey='" + ciphertextDataKey + '\'' + '}';
    }
}
