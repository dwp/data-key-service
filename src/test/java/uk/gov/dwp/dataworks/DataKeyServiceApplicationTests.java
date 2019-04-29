package uk.gov.dwp.dataworks;

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
import uk.gov.dwp.dataworks.errors.CurrentKeyIdFailure;
import uk.gov.dwp.dataworks.errors.DataKeyDecryptionFailure;
import uk.gov.dwp.dataworks.errors.DataKeyGenerationFailure;
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
public class DataKeyServiceApplicationTests {

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
    public void contextLoads() {
    }

    @Test
    public void generateDataKeyRequest() throws Exception {
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
    public void decryptRequest() throws Exception {
        DecryptDataKeyResponse response = new DecryptDataKeyResponse("decryptKeyId", "iv", "plaintextDataKey");
        given(dataKeyDecryptionProvider.decryptDataKey("encryptionKeyId", "cipher text"))
                .willReturn(response);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", "encryptionKeyId").content("cipher text"))
                .andExpect(status().isOk())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(response)));
    }

    @Test
    public void currentKeyIdProviderThrowsError() throws Exception {
        given(currentKeyIdProvider.getKeyId()).willThrow(new CurrentKeyIdFailure());

        mockMvc.perform(get("/datakey"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void dataKeyGeneratorProviderThrowsError() throws Exception {
        given(dataKeyGeneratorProvider.generateDataKey(any())).willThrow(new DataKeyGenerationFailure());

        mockMvc.perform(get("/datakey"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void setDataKeyDecryptionProviderThrowsError() throws Exception {
        given(dataKeyDecryptionProvider.decryptDataKey(any(), any())).willThrow(new DataKeyDecryptionFailure());

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", "myKeyId").content("my content to decrypt"))
                .andExpect(status().isInternalServerError());
    }
}
