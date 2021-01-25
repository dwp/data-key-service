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
import uk.gov.dwp.dataworks.util.CertificateUtils;

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

    private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
    private final DataKeyService dataKeyService;
    private final CertificateUtils certificateUtils;
    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(DataKeyController.class.toString());

    @Autowired
    public DataKeyController(DataKeyService dataKeyService, CertificateUtils certificateUtils) {
        this.dataKeyService = dataKeyService;
        this.certificateUtils = certificateUtils;
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
        LOGGER.info("Remote client info",
                new Pair<>("remote_client_ip", servletRequest.getHeader(X_FORWARDED_FOR)),
                new Pair<>("remote_client_address", servletRequest.getRemoteAddr()));
        certificateUtils.checkCertificatesAgainstCrl(requestCertificates(servletRequest));
        String keyId = dataKeyService.currentKeyId(correlationId);
        return new ResponseEntity<>(dataKeyService.generate(keyId, correlationId), HttpStatus.CREATED);
    }


    @RequestMapping(value = "/actions/decrypt", method = RequestMethod.POST)
    @ApiOperation(value = "Tries to decrypt the ciphertext of a data key", response = DecryptDataKeyResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully decrypted the data key"),
            @ApiResponse(code = 400, message = "The supplied data key could not be decrypted. Either the ciphertext is invalid or the data key encryption key is incorrect."),
            @ApiResponse(code = 403, message = "Client used a revoked certificate"),
            @ApiResponse(code = 503, message = "There has been an internal error, or a dependency failure")
    })
    public DecryptDataKeyResponse decrypt(
            @RequestParam(name = "keyId") String dataKeyEncryptionKeyId,
            @RequestParam(name = "correlationId", defaultValue = "NOT_SET") String correlationId,
            @RequestBody String ciphertextDataKey,
            HttpServletRequest servletRequest) throws MasterKeystoreException {
        certificateUtils.checkCertificatesAgainstCrl(requestCertificates(servletRequest));
        LOGGER.info("Remote client info",
                new Pair<>("remote_client_ip", servletRequest.getHeader(X_FORWARDED_FOR)),
                new Pair<>("remote_client_address", servletRequest.getRemoteAddr()));
        return dataKeyService.decrypt(dataKeyEncryptionKeyId, ciphertextDataKey, correlationId);
    }

    private Certificate[] requestCertificates(HttpServletRequest servletRequest) {
        return (Certificate[]) servletRequest.getAttribute("javax.servlet.request.X509Certificate");
    }

}
