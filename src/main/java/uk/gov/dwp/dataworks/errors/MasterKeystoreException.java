package uk.gov.dwp.dataworks.errors;

import com.cavium.cfm2.CFM2Exception;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class MasterKeystoreException extends Exception {

    public MasterKeystoreException(String message) {
        super(message);
    }

    public MasterKeystoreException(String message, Throwable e) {
        super(message, e);
    }
}
