package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class DataKeyGenerationException extends RuntimeException {

    public DataKeyGenerationException() {
        super("Failed to generate a new data key due to an internal error. Try again later.");
    }
}
