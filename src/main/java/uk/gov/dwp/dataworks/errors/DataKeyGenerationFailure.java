package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class DataKeyGenerationFailure extends RuntimeException {
    public DataKeyGenerationFailure() {
        super("Failed to generate a new data key due to an internal error. Try again later.");
    }
}
