package uk.gov.dwp.dataworks.controller;

import org.junit.Test;
import uk.gov.dwp.dataworks.errors.MasterKeystoreException;
import uk.gov.dwp.dataworks.service.DataKeyService;

import javax.servlet.http.HttpServletRequest;

import java.security.cert.Certificate;

import static org.mockito.Mockito.*;

public class DataKeyControllerTest {

    private final String correlationId = "correlation";

    @Test
    public void generateWillPassParametersToTheDataKeyService() throws MasterKeystoreException {
        DataKeyService mockDataKeyService = mock(DataKeyService.class);
        DataKeyController dataKeyController = new DataKeyController(mockDataKeyService);
        String keyId = "I am a key Id";
        when(mockDataKeyService.currentKeyId(correlationId)).thenReturn(keyId);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenReturn(new Certificate[] {});
        dataKeyController.generate(correlationId, request);

        verify(mockDataKeyService, times(1)).currentKeyId(eq(correlationId));
        verify(mockDataKeyService, times(1)).generate(eq(keyId), eq(correlationId));
        verifyNoMoreInteractions(mockDataKeyService);
    }

    @Test
    public void decryptWillPassParametersToTheDataKeyService() throws MasterKeystoreException {
        DataKeyService mockDataKeyService = mock(DataKeyService.class);
        DataKeyController dataKeyController = new DataKeyController(mockDataKeyService);
        String keyId = "I am a key Id";
        String ciphertextDataKey = "I am a ciphertext Data Key";
        dataKeyController.decrypt(keyId, correlationId, ciphertextDataKey);

        verify(mockDataKeyService, times(1)).decrypt(eq(keyId), eq(ciphertextDataKey), eq(correlationId));
        verifyNoMoreInteractions(mockDataKeyService);
    }
}
