package uk.gov.dwp.dataworks.dto;

import io.swagger.annotations.ApiModelProperty;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class HealthCheckResponse {

    public enum Health {OK, BAD}

    @ApiModelProperty(notes = "Can the controller see its dependencies.")
    private Health encryptionService;

    @ApiModelProperty(notes = "Can the controller fetch the current master key.")
    private Health masterKey;

    @ApiModelProperty(notes = "Can the controller generate a new data key.")
    private Health dataKeyGenerator;

    @ApiModelProperty(notes = "Can the controller encrypt a new data key.")
    private Health encryption;

    @ApiModelProperty(notes = "Can the controller decrypt an encrypted data key.")
    private Health decryption;

    @ApiModelProperty(notes = "DKS Correlation Id passed in from the client call")
    private String correlationId = "NOT_SET";

    private Map<String, String> trustedCertificates;

    public HealthCheckResponse() {
        this.encryptionService = Health.BAD;
        this.masterKey = Health.BAD;
        this.dataKeyGenerator = Health.BAD;
        this.encryption = Health.BAD;
        this.decryption = Health.BAD;
        this.trustedCertificates = new HashMap<>();
    }

    public HealthCheckResponse(Health encryptionService,
                               Health masterKey,
                               Health dataKeyGenerator,
                               Health encryption,
                               Health decryption) {
        this.encryptionService = encryptionService;
        this.masterKey = masterKey;
        this.dataKeyGenerator = dataKeyGenerator;
        this.encryption = encryption;
        this.decryption = decryption;
        this.trustedCertificates = new HashMap<>();
    }

    public boolean areAllServicesOk() {
        return encryptionService == Health.OK
                && masterKey == Health.OK
                && dataKeyGenerator == Health.OK
                && encryption == Health.OK
                && decryption == Health.OK;
    }

    public Health getEncryptionService() {
        return encryptionService;
    }

    public void setEncryptionService(Health encryptionService) {
        this.encryptionService = encryptionService;
    }

    public Health getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(Health masterKey) {
        this.masterKey = masterKey;
    }

    public Health getDataKeyGenerator() {
        return dataKeyGenerator;
    }

    public void setDataKeyGenerator(Health dataKeyGenerator) {
        this.dataKeyGenerator = dataKeyGenerator;
    }

    public Health getEncryption() {
        return encryption;
    }

    public void setEncryption(Health encryption) {
        this.encryption = encryption;
    }

    public Health getDecryption() {
        return decryption;
    }

    public void setDecryption(Health decryption) {
        this.decryption = decryption;
    }

    public Map<String, String> getTrustedCertificates() {
        return trustedCertificates;
    }

    public void setTrustedCertificates(Map<String, String> trustedCertificates) {
        this.trustedCertificates = trustedCertificates;
    }

    public String getcorrelationId() {
        return correlationId;
    }

    public void setcorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public HealthCheckResponse withcorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

}
