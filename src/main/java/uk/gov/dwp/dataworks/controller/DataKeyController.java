package uk.gov.dwp.dataworks.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import kotlin.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.errors.RevokedClientCertificateException;
import uk.gov.dwp.dataworks.logging.DataworksLogger;
import uk.gov.dwp.dataworks.service.DataKeyService;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.cert.*;
import java.util.Arrays;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/datakey")
@Api(value = "datakey")
public class DataKeyController {

    private final DataKeyService dataKeyService;
    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(DataKeyController.class.toString());

    @Autowired
    public DataKeyController(DataKeyService dataKeyService) {
        this.dataKeyService = dataKeyService;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ApiOperation(value = "Generate a new data key", response = GenerateDataKeyResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created a new data key"),
            @ApiResponse(code = 403, message = "Client used a revoked certificate"),
            @ApiResponse(code = 503, message = "There has been an internal error, or a dependency failure")
    })
    public ResponseEntity<GenerateDataKeyResponse> generate(@RequestParam(name = "correlationId", defaultValue = "NOT_SET")
            String correlationId, HttpServletRequest servletRequest) throws MasterKeystoreException {
        Certificate[] certs =
                (Certificate[]) servletRequest.getAttribute("javax.servlet.request.X509Certificate");
        if (certs != null) {
            Arrays.asList(certs).forEach(this::checkRevocation);
        }
        String keyId = dataKeyService.currentKeyId(correlationId);
        return new ResponseEntity<>(dataKeyService.generate(keyId, correlationId), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/actions/decrypt", method = RequestMethod.POST)
    @ApiOperation(value = "Tries to decrypt the ciphertext of a data key", response = DecryptDataKeyResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully decrypted the data key"),
            @ApiResponse(code = 400, message = "The supplied data key could not be decrypted. Either the ciphertext is invalid or the data key encryption key is incorrect."),
            @ApiResponse(code = 503, message = "There has been an internal error, or a dependency failure")
    })
    public DecryptDataKeyResponse decrypt(
            @RequestParam(name = "keyId") String dataKeyEncryptionKeyId,
            @RequestParam(name = "correlationId", defaultValue = "NOT_SET") String correlationId,
            @RequestBody String ciphertextDataKey) throws MasterKeystoreException {
        return dataKeyService.decrypt(dataKeyEncryptionKeyId, ciphertextDataKey, correlationId);
    }

    private void checkRevocation(Certificate certificate) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            X509CRL crl = (X509CRL) cf.generateCRL(new FileInputStream(new File("resources/development.crl")));
            X509CRLEntry revocationEntry = crl.getRevokedCertificate(((X509Certificate) certificate).getSerialNumber());
            if (revocationEntry != null) {
                System.out.println("Revoked");
            }
            else {
                String serialNumber = ((X509Certificate) certificate).getSerialNumber().toString();
                LOGGER.error("Client attempted to use service with revoked certificate",
                        new Pair("serial_number", serialNumber));
                throw new RevokedClientCertificateException(serialNumber);
            }
        }
        catch (CertificateException | CRLException | FileNotFoundException e) {
            throw new RevokedClientCertificateException(e.getMessage());
        }
    }
}
