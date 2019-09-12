package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class LoginException extends RuntimeException {
    public LoginException(Throwable e) {
        super(e);
    }

    public LoginException(String s) {
        super(s);
    }
}
