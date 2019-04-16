package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class DataKeyDecryptionFailure extends RuntimeException {
    public DataKeyDecryptionFailure() {
        super("Failed to decrypt this data key due to an internal error. Try again later.");
    }
}
