package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnusableParameterException extends RuntimeException {

    public UnusableParameterException(String correlationId) {
        super("The supplied key or cyphertext are unusable. correlation_id: " + correlationId);
    }
}
