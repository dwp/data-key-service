package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class DataKeyDecryptionException extends RuntimeException {

    public DataKeyDecryptionException() {
        super("Failed to decrypt this data key due to an internal error. Try again later.");
    }
}
