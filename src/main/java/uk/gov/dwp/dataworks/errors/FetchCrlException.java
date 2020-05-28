package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class FetchCrlException extends Exception {
    public FetchCrlException(Throwable e) {
        super(e);
    }
}
