package uk.gov.dwp.dataworks.controller;

import org.junit.Test;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.service.DataKeyService;

import static org.mockito.Mockito.*;

public class DataKeyControllerTest {

    @Test
    public void generateWillPassParametersToTheDataKeyService() throws MasterKeystoreException {
        DataKeyService mockDataKeyService = mock(DataKeyService.class);
        DataKeyController dataKeyController = new DataKeyController(mockDataKeyService);
        String keyId = "I am a key Id";
        String dksCorrelationId = "aaa111";
        when(mockDataKeyService.currentKeyId()).thenReturn(keyId);

        dataKeyController.generate(dksCorrelationId);

        verify(mockDataKeyService, times(1)).currentKeyId();
        verify(mockDataKeyService, times(1)).generate(eq(keyId));
        verifyNoMoreInteractions(mockDataKeyService);
    }

    @Test
    public void decryptWillPassParametersToTheDataKeyService() throws MasterKeystoreException {
        DataKeyService mockDataKeyService = mock(DataKeyService.class);
        DataKeyController dataKeyController = new DataKeyController(mockDataKeyService);
        String keyId = "I am a key Id";
        String ciphertextDataKey = "I am a ciphertext Data Key";
        String dksCorrelationId = "aaa111";

        dataKeyController.decrypt(keyId, dksCorrelationId, ciphertextDataKey);

        verify(mockDataKeyService, times(1)).decrypt(keyId, ciphertextDataKey);
        verifyNoMoreInteractions(mockDataKeyService);
    }
}
