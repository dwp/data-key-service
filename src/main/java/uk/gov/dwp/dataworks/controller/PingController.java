package uk.gov.dwp.dataworks.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/ping")
@Api(value="ping")
public class PingController {

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="A simple endpoint to confirm that the service is running.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Service is running")
    })
    public String ping() {
        // Nothing to do, just saying hello!
        return null;
    }
}
