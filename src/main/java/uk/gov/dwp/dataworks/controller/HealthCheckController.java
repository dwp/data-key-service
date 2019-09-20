package uk.gov.dwp.dataworks.controller;

import com.google.common.base.Strings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.dto.HealthCheckResponse;
import uk.gov.dwp.dataworks.service.DataKeyService;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/healthcheck")
@Api(value="healthcheck")
public class HealthCheckController {

    private final DataKeyService dataKeyService;
    private final static Logger LOGGER = LoggerFactory.getLogger(DataKeyController.class);

    @Autowired
    public HealthCheckController(DataKeyService dataKeyService) {
        this.dataKeyService = dataKeyService;
    }

    @PreAuthorize("hasAuthority('ROLE_USER')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ApiOperation(value="A simple endpoint to confirm that the service is well configured and that its dependencies are fulfilled.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Service is healthy, all dependencies fine, can fulfill requests."),
            @ApiResponse(code = 500, message = "Service is unhealthy, one or more dependencies can't be fullfilled. Response body indicates which.")
    })

    public ResponseEntity<HealthCheckResponse> healthCheck() {


        boolean canReachDependencies = false;
        boolean canRetrieveCurrentMasterKeyId = false;
        boolean canCreateNewDataKey = false;
        boolean canEncryptDataKey = false;
        boolean canDecryptDataKey = false;
        HealthCheckResponse health = new HealthCheckResponse();
        try {
            Map<String, String> trustedCertificates = new HashMap<>();

            if (StringUtils.isNoneBlank(trustStorePath, trustStorePassword)) {
                KeyStore keystore = KeyStore.getInstance("JKS");
                keystore.load(new FileInputStream(trustStorePath), trustStorePassword.toCharArray());
                Enumeration<String> aliases = keystore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Certificate certificate = keystore.getCertificate(alias);
                    String thumbprint = DigestUtils.sha1Hex(certificate.getEncoded());
                    trustedCertificates.put(alias, thumbprint.replaceAll("(..)", "$1:").toUpperCase());
                }
            }

            health.setTrustedCertificates(trustedCertificates);
            canReachDependencies = dataKeyService != null && dataKeyService.canSeeDependencies();
            String currentKeyId = this.dataKeyService.currentKeyId();
            canRetrieveCurrentMasterKeyId = ! Strings.isNullOrEmpty(currentKeyId);
            GenerateDataKeyResponse encryptResponse = dataKeyService.generate(dataKeyService.currentKeyId());
            canCreateNewDataKey = ! Strings.isNullOrEmpty(encryptResponse.dataKeyEncryptionKeyId) &&
                                    ! Strings.isNullOrEmpty(encryptResponse.plaintextDataKey);
            canEncryptDataKey = ! Strings.isNullOrEmpty(encryptResponse.ciphertextDataKey);
            DecryptDataKeyResponse decryptResponse =
                    dataKeyService.decrypt(encryptResponse.dataKeyEncryptionKeyId, encryptResponse.ciphertextDataKey);
            canDecryptDataKey = ! Strings.isNullOrEmpty(decryptResponse.plaintextDataKey) &&
                    decryptResponse.plaintextDataKey.equals(encryptResponse.plaintextDataKey);
        }
        finally {
            health.setEncryptionService(canReachDependencies ? HealthCheckResponse.Health.OK : HealthCheckResponse.Health.BAD);
            health.setMasterKey(canRetrieveCurrentMasterKeyId ? HealthCheckResponse.Health.OK : HealthCheckResponse.Health.BAD);
            health.setDataKeyGenerator(canCreateNewDataKey ? HealthCheckResponse.Health.OK : HealthCheckResponse.Health.BAD);
            health.setEncryption(canEncryptDataKey ? HealthCheckResponse.Health.OK : HealthCheckResponse.Health.BAD);
            health.setDecryption(canDecryptDataKey ? HealthCheckResponse.Health.OK : HealthCheckResponse.Health.BAD);

            boolean allOk = canReachDependencies && canRetrieveCurrentMasterKeyId &&
                    canCreateNewDataKey && canEncryptDataKey &&  canDecryptDataKey;

            return new ResponseEntity<>(health, allOk ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Value("${server.ssl.trust-store:}")
    private String trustStorePath;

    @Value("${server.ssl.trust-store-password:}")
    private String trustStorePassword;

}
