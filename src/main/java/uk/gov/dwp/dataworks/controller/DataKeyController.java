package uk.gov.dwp.dataworks.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.service.DataKeyService;

import javax.servlet.http.HttpServletRequest;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/datakey")
@Api(value = "datakey")
public class DataKeyController {

    @Autowired
    private ServletWebServerApplicationContext server;

    private final DataKeyService dataKeyService;

    @Autowired
    public DataKeyController(DataKeyService dataKeyService) {
        this.dataKeyService = dataKeyService;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ApiOperation(value = "Generate a new data key", response = GenerateDataKeyResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created a new data key"),
            @ApiResponse(code = 503, message = "There has been an internal error, or a dependency failure")
    })
    public ResponseEntity<GenerateDataKeyResponse> generate(HttpServletRequest request,
            @RequestParam(name = "correlationId", defaultValue = "NOT_SET") String correlationId) throws MasterKeystoreException {
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

}
