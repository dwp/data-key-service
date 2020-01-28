package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CurrentKeyIdException extends RuntimeException {

    public CurrentKeyIdException(String correlationId) {
        super("Failed to retrieve the current key id. correlation_id: " + correlationId);
    }
}
