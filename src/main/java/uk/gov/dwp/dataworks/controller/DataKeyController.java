package uk.gov.dwp.dataworks.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.service.DataKeyService;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/datakey")
@Api(value="datakey")
public class DataKeyController {

    private final DataKeyService dataKeyService;

    @Autowired
    public DataKeyController(DataKeyService dataKeyService) {
        this.dataKeyService = dataKeyService;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ApiOperation(value="Generate a new data key", response=GenerateDataKeyResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created a new data key"),
            @ApiResponse(code = 503, message = "There has been an internal error, or a dependency failure")
    })
    public ResponseEntity<GenerateDataKeyResponse> generate() {
        return new ResponseEntity<>(dataKeyService.generate(), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/actions/decrypt", method = RequestMethod.POST)
    @ApiOperation(value="Tries to decrypt the ciphertext of a data key", response=DecryptDataKeyResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully decrypted the data key"),
            @ApiResponse(code = 400, message = "The supplied data key could not be decrypted. Either the ciphertext is invalid or the data key encryption key is incorrect."),
            @ApiResponse(code = 503, message = "There has been an internal error, or a dependency failure")
    })    public DecryptDataKeyResponse decrypt(
            @RequestParam(value = "keyId") String dataKeyEncryptionKeyId,
            @RequestBody String ciphertextDataKey) {
        return dataKeyService.decrypt(dataKeyEncryptionKeyId, ciphertextDataKey);
    }


}
