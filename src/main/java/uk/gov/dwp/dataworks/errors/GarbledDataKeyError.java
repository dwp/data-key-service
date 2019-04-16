package uk.gov.dwp.dataworks.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class GarbledDataKeyError extends RuntimeException{
    public GarbledDataKeyError() {
        super("The supplied data key could not be decrypted. Either the ciphertext is invalid or the data key encryption key is incorrect.");
    }
}
