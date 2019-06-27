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
import uk.gov.dwp.dataworks.errors.*;
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
@ActiveProfiles({"IntegrationTest", "INSECURE"})
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

        GenerateDataKeyResponse response =
                new GenerateDataKeyResponse("encryptionId",
                        "plainKey",
                        "cipherKey");

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
        DecryptDataKeyResponse response =
                new DecryptDataKeyResponse("decryptKeyId",
                        "plaintextDataKey");

        String dataKeyEncryptionKeyId = "dataKeyEncryptionKeyId";
        String encryptedDataKey = "blah blah blah";
        given(dataKeyDecryptionProvider.decryptDataKey(dataKeyEncryptionKeyId, encryptedDataKey))
                .willReturn(response);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}",
                dataKeyEncryptionKeyId).content(encryptedDataKey))
                .andExpect(status().isOk())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(response)));
    }

    @Test
    public void shouldReturnServiceUnavailableWhenCurrentKeyIdIsUnavailable() throws Exception {
        given(currentKeyIdProvider.getKeyId()).willThrow(CurrentKeyIdException.class);
        mockMvc.perform(get("/datakey")).andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnServiceUnavailableWhenDataKeyGenerationFails() throws Exception {
        given(dataKeyGeneratorProvider.generateDataKey(any())).willThrow(DataKeyGenerationException.class);
        mockMvc.perform(get("/datakey")).andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnServiceUnavailableWhenDataKeyDecryptionFails() throws Exception {
        String dataKeyEncryptionKeyId = "dataKeyEncryptionKeyId";
        given(dataKeyDecryptionProvider.decryptDataKey(any(), any()))
                .willThrow(DataKeyDecryptionException.class);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", dataKeyEncryptionKeyId)
                .content("my content to decrypt"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnBadRequestWhenEncryptedDataKeyIsInvalid() throws Exception {
        given(dataKeyDecryptionProvider.decryptDataKey(any(), any()))
                .willThrow(GarbledDataKeyException.class);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", "DATAKEYID")
                .content("my garbled content to decrypt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenEncryptedDataKeyTooLarge() throws Exception {
        given(dataKeyDecryptionProvider.decryptDataKey("DATAKEYID", "my garbled content to decrypt"))
                .willThrow(UnusableParameterException.class);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", "DATAKEYID")
                .content("my garbled content to decrypt"))
                .andExpect(status().isBadRequest());
    }
}
