package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class RevokedClientCertificateException extends RuntimeException {

    public RevokedClientCertificateException(String correlationId) {
        super("Failed to generate a new data key due to an internal error. Try again later. correlation_id: " + correlationId);
    }
}
