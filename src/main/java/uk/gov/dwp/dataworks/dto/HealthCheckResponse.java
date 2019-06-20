package uk.gov.dwp.dataworks.dto;

@SuppressWarnings("unused")
public class HealthCheckResponse {

    public enum Health {OK, BAD}

    public HealthCheckResponse() {
        this.encryptionService = Health.BAD;
        this.masterKey = Health.BAD;
        this.dataKeyGenerator = Health.BAD;
        this.encryption = Health.BAD;
        this.decryption = Health.BAD;
    }

    public HealthCheckResponse(Health encryptionService,
            Health masterKey, Health dataKeyGenerator, Health encryption,
            Health decryption) {
        this.encryptionService = encryptionService;
        this.masterKey = masterKey;
        this.dataKeyGenerator = dataKeyGenerator;
        this.encryption = encryption;
        this.decryption = decryption;
    }

    private Health encryptionService;
    private Health masterKey;
    private Health dataKeyGenerator;
    private Health encryption;
    private Health decryption;

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

}
