package uk.gov.dwp.dataworks.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionException;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationException;
import uk.gov.dwp.dataworks.errors.GarbledDataKeyException;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles("Test")
@AutoConfigureMockMvc
public class DataKeyIntegrationTests {

    @Autowired
    private CurrentKeyIdProvider currentKeyIdProvider;

    @Autowired
    private DataKeyGeneratorProvider dataKeyGeneratorProvider;

    @Autowired
    private DataKeyDecryptionProvider dataKeyDecryptionProvider;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setup() {
        Mockito.reset(currentKeyIdProvider, dataKeyGeneratorProvider, dataKeyDecryptionProvider);
    }

    @Test
    public void shouldReturnADataKeyWhenRequestingKeyGeneration() throws Exception {
        given(currentKeyIdProvider.getKeyId()).willReturn("myKeyId");

        GenerateDataKeyResponse response = new GenerateDataKeyResponse("encryptionId", "plainKey", "cipherKey");

        given(dataKeyGeneratorProvider.generateDataKey("myKeyId"))
                .willReturn(response);

        mockMvc.perform(get("/datakey"))
                .andExpect(status().isCreated())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(response)));

        // Must also allow a trailing slash
        mockMvc.perform(get("/datakey/"))
                .andExpect(status().isCreated())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(response)));
    }

    @Test
    public void shouldReturnDecryptedKeyWhenRequested() throws Exception {
        DecryptDataKeyResponse response = new DecryptDataKeyResponse("decryptKeyId", "iv", "plaintextDataKey");
        given(dataKeyDecryptionProvider.decryptDataKey("encryptionKeyId", "cipher text"))
                .willReturn(response);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", "encryptionKeyId").content("cipher text"))
                .andExpect(status().isOk())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(response)));
    }

    @Test
    public void shouldReturnServiceUnavailableWhenCurrentKeyIdIsUnavailable() throws Exception {
        given(currentKeyIdProvider.getKeyId()).willThrow(new CurrentKeyIdException());

        mockMvc.perform(get("/datakey"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnServiceUnavailableWhenDataKeyGenerationFails() throws Exception {
        given(dataKeyGeneratorProvider.generateDataKey(any())).willThrow(new DataKeyGenerationException());

        mockMvc.perform(get("/datakey"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnServiceUnavailableWhenDataKeyDecryptionFails() throws Exception {
        given(dataKeyDecryptionProvider.decryptDataKey(any(), any())).willThrow(new DataKeyDecryptionException());

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", "myKeyId").content("my content to decrypt"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnBadRequestWhenEncryptedDataKeyIsInvalid() throws Exception {
        given(dataKeyDecryptionProvider.decryptDataKey("myKeyId", "my garbled content to decrypt")).willThrow(new GarbledDataKeyException());

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", "myKeyId").content("my garbled content to decrypt"))
                .andExpect(status().isBadRequest());
    }
}
