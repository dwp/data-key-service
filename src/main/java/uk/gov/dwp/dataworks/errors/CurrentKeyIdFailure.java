package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CurrentKeyIdFailure extends RuntimeException  {
    public CurrentKeyIdFailure() {
        super("Failed to retrieve the current key id. Try again later.");
    }
}
