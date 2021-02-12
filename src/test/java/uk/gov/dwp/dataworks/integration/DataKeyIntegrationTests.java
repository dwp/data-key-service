package uk.gov.dwp.dataworks.integration;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import uk.gov.dwp.dataworks.config.InsecureConfiguration;
import uk.gov.dwp.dataworks.controller.DataKeyController;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.errors.*;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.service.DataKeyService;
import uk.gov.dwp.dataworks.util.CertificateUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        DataKeyController.class,
        DataKeyService.class,
        CertificateUtils.class,
        InsecureConfiguration.class })
@ActiveProfiles({"INSECURE"})
@AutoConfigureMockMvc
@EnableWebMvc
public class DataKeyIntegrationTests {

    private final String correlationId = "correlation";

    @MockBean
    private AmazonS3 amazonS3;

    @MockBean
    private CurrentKeyIdProvider currentKeyIdProvider;

    @MockBean
    private DataKeyGeneratorProvider dataKeyGeneratorProvider;

    @MockBean
    private DataKeyDecryptionProvider dataKeyDecryptionProvider;


    @Autowired
    private MockMvc mockMvc;

//    @Autowired
//    private CacheManager cacheManager;

    @Before
    public void setup() {
        Mockito.reset(currentKeyIdProvider, dataKeyGeneratorProvider, dataKeyDecryptionProvider);
//        for (String name : cacheManager.getCacheNames()) {
//            Objects.requireNonNull(cacheManager.getCache(name)).clear();
//        }
    }

    @Test
    public void shouldReturnADataKeyWhenRequestingKeyGenerationWithoutSpecifiedCorrelationId() throws Exception {
        given(currentKeyIdProvider.getKeyId(anyString())).willReturn("myKeyId");

        GenerateDataKeyResponse response =
                new GenerateDataKeyResponse("encryptionId",
                        "plainKey",
                        "cipherKey");

        given(dataKeyGeneratorProvider.generateDataKey(eq("myKeyId"), anyString()))
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
    public void shouldReturnADataKeyWhenRequestingKeyGenerationWithSpecifiedCorrelationId() throws Exception {
        given(currentKeyIdProvider.getKeyId(eq(correlationId))).willReturn("myKeyId");

        GenerateDataKeyResponse response =
                new GenerateDataKeyResponse("encryptionId",
                        "plainKey",
                        "cipherKey")
                .withCorrelationId(correlationId);

        given(dataKeyGeneratorProvider.generateDataKey(eq("myKeyId"), eq(correlationId)))
                .willReturn(response);

        mockMvc.perform(get("/datakey?correlationId={correlationId}", correlationId))
                .andExpect(status().isCreated())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(response)));

        // Must also allow a trailing slash
        mockMvc.perform(get("/datakey/?correlationId={correlationId}", correlationId))
                .andExpect(status().isCreated())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(response)));
    }

    @Test
    public void shouldReturnDecryptedKeyWhenRequestedWithoutSpecifiedCorrelationId() throws Exception {
        DecryptDataKeyResponse response =
                new DecryptDataKeyResponse("decryptKeyId",
                        "plaintextDataKey");

        String dataKeyEncryptionKeyId = "dataKeyEncryptionKeyId";
        String encryptedDataKey = "blah blah blah";
        given(dataKeyDecryptionProvider.decryptDataKey(eq(dataKeyEncryptionKeyId), eq(encryptedDataKey), anyString()))
                .willReturn(response);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}",
                dataKeyEncryptionKeyId).content(encryptedDataKey))
                .andExpect(status().isOk())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(response)));
    }

    @Test
    public void shouldReturnDecryptedKeyWhenRequestedWithSpecifiedCorrelationId() throws Exception {
        DecryptDataKeyResponse response =
                new DecryptDataKeyResponse("decryptKeyId", "plaintextDataKey")
                        .withCorrelationId(correlationId);

        String dataKeyEncryptionKeyId = "dataKeyEncryptionKeyId";
        String encryptedDataKey = "blah blah blah";
        given(dataKeyDecryptionProvider.decryptDataKey(eq(dataKeyEncryptionKeyId), eq(encryptedDataKey), eq(correlationId)))
                .willReturn(response);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}&correlationId={correlationId}", dataKeyEncryptionKeyId, correlationId)
                .content(encryptedDataKey))
                .andExpect(status().isOk())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(response)));
    }

    @Test
    public void shouldReturnServiceUnavailableWhenCurrentKeyIdIsUnavailableWithoutSpecifiedCorrelationId() throws Exception {
        given(currentKeyIdProvider.getKeyId(anyString())).willThrow(CurrentKeyIdException.class);

        mockMvc.perform(get("/datakey")).andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnServiceUnavailableWhenCurrentKeyIdIsUnavailableWithSpecifiedCorrelationId() throws Exception {
        given(currentKeyIdProvider.getKeyId(eq(correlationId))).willThrow(CurrentKeyIdException.class);

        mockMvc.perform(get("/datakey?correlationId={correlationId}", correlationId)).andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnServiceUnavailableWhenDataKeyGenerationFailsWithoutSpecifiedCorrelationId() throws Exception {
        given(currentKeyIdProvider.getKeyId(anyString())).willReturn("key-id");
        given(dataKeyGeneratorProvider.generateDataKey(eq("key-id"), anyString())).willThrow(DataKeyGenerationException.class);

        mockMvc.perform(get("/datakey")).andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnServiceUnavailableWhenDataKeyGenerationFailsWithCorrelationId() throws Exception {
        given(currentKeyIdProvider.getKeyId(eq(correlationId))).willReturn("key-id");
        given(dataKeyGeneratorProvider.generateDataKey(eq("key-id"), eq(correlationId))).willThrow(DataKeyGenerationException.class);

        mockMvc.perform(get("/datakey?correlationId={correlationId}", correlationId)).andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnServiceUnavailableWhenDataKeyDecryptionFails() throws Exception {
        String dataKeyEncryptionKeyId = "dataKeyEncryptionKeyId";
        given(dataKeyDecryptionProvider.decryptDataKey(anyString(), anyString(), anyString()))
                .willThrow(DataKeyDecryptionException.class);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", dataKeyEncryptionKeyId)
                .content("my content to decrypt"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    public void shouldReturnBadRequestWhenEncryptedDataKeyIsInvalid() throws Exception {
        given(dataKeyDecryptionProvider.decryptDataKey(anyString(), anyString(), anyString()))
                .willThrow(GarbledDataKeyException.class);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", "DATAKEYID")
                .content("my garbled content to decrypt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenEncryptedDataKeyTooLarge() throws Exception {
        given(dataKeyDecryptionProvider.decryptDataKey(eq("DATAKEYID"), eq("my garbled content to decrypt"), anyString()))
                .willThrow(UnusableParameterException.class);

        mockMvc.perform(post("/datakey/actions/decrypt?keyId={keyId}", "DATAKEYID")
                .content("my garbled content to decrypt"))
                .andExpect(status().isBadRequest());
    }
}
