package uk.gov.dwp.dataworks.controller;

import org.junit.Test;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.service.DataKeyService;

import static org.mockito.Mockito.*;

public class DataKeyControllerTest {

    @Test
    public void decryptWillPassParametersToTheDataKeyService() throws MasterKeystoreException {
        DataKeyService dataKeyService = mock(DataKeyService.class);
        DataKeyController dataKeyController = new DataKeyController(dataKeyService);
        String keyId = "I am a key Id";
        String ciphertextDataKey = "I am a ciphertext Data Key";

        dataKeyController.decrypt(keyId, ciphertextDataKey);

        verify(dataKeyService, times(1)).decrypt(keyId, ciphertextDataKey);
    }
}
