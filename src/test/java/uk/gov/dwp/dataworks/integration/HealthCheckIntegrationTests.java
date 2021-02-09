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
import uk.gov.dwp.dataworks.controller.HealthCheckController;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.dto.HealthCheckResponse;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.service.DataKeyService;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { HealthCheckController.class, DataKeyService.class,
        InsecureConfiguration.class, AtomicInteger.class })
@ActiveProfiles({"INSECURE"})
@AutoConfigureMockMvc
@EnableWebMvc
public class HealthCheckIntegrationTests {


    @Before
    public void setup() {
        Mockito.reset(currentKeyIdProvider, dataKeyGeneratorProvider, dataKeyDecryptionProvider);
        gauge.set(0);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrentKeyIdProvider currentKeyIdProvider;

    @MockBean
    private DataKeyGeneratorProvider dataKeyGeneratorProvider;

    @MockBean
    private DataKeyDecryptionProvider dataKeyDecryptionProvider;

    @Autowired
    private AtomicInteger gauge;

    @MockBean
    private AmazonS3 amazonS3;

    private final static String HEALTHCHECK_ENDPOINT = "/healthcheck";
    private final static String ENCRYPTION_KEY_ID = "ENCRYPTION_KEY_ID";
    private final static String PLAIN_TEXT_KEY = "PLAIN_TEXT_KEY";
    private final static String CIPHER_TEXT_DATA_KEY = "CIPHER_TEXT_DATA_KEY";

    @Test
    public void shouldGiveServerErrorCurrentKeyIdProviderDependencyCheckFails() throws Exception {
        shouldGiveServerErrorWhenDependencyCheckFails(false,
                true,
                true);
    }

    @Test
    public void shouldGiveServerErrorDataKeyGeneratorProviderDependencyCheckFails() throws Exception {
        shouldGiveServerErrorWhenDependencyCheckFails(true,
                false,
                true);
    }

    @Test
    public void shouldGiveServerErrorDataKeyDecryptionProviderDependencyCheckFails() throws Exception {
        shouldGiveServerErrorWhenDependencyCheckFails(true,
                true,
                false);
    }

    private void shouldGiveServerErrorWhenDependencyCheckFails(
            boolean currentKeyIdDependencies,
            boolean dataKeyGeneratorDependencies,
            boolean dataKeyDecryptionDependencies) throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(currentKeyIdDependencies);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(dataKeyGeneratorDependencies);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(dataKeyDecryptionDependencies);
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD);
        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
        assertEquals(1, gauge.get());
    }

    @Test
    public void shouldGiveServerErrorWhenMasterKeyCheckFails() throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId(anyString())).willThrow(RuntimeException.class);

        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD);
        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
        assertEquals(1, gauge.get());
    }

    @Test
    public void shouldGiveServerErrorWhenMasterKeyCheckGivesEmptyKey() throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId(anyString())).willReturn("");
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD);
        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
        assertEquals(1, gauge.get());
    }

    @Test
    public void shouldGiveServerErrorWhenGenerateKeyFails() throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId(anyString())).willReturn(ENCRYPTION_KEY_ID);
        given(dataKeyGeneratorProvider.generateDataKey(eq(ENCRYPTION_KEY_ID), anyString())).willThrow(RuntimeException.class);

        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD);

        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
        assertEquals(1, gauge.get());
    }

    @Test
    public void shouldGiveServerErrorWhenDecryptionFails() throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId(anyString())).willReturn(ENCRYPTION_KEY_ID);
        String correlationId = "correlation";
        GenerateDataKeyResponse response =
                new GenerateDataKeyResponse(ENCRYPTION_KEY_ID, PLAIN_TEXT_KEY, CIPHER_TEXT_DATA_KEY)
                .withCorrelationId(correlationId);

        given(dataKeyGeneratorProvider.generateDataKey(ENCRYPTION_KEY_ID, correlationId)).willReturn(response);

        given(dataKeyDecryptionProvider.decryptDataKey(eq(ENCRYPTION_KEY_ID), eq(CIPHER_TEXT_DATA_KEY), anyString()))
                .willThrow(RuntimeException.class);

        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.BAD)
                .withCorrelationId(correlationId);

        mockMvc.perform(get(HEALTHCHECK_ENDPOINT + "?correlationId={correlationId}", correlationId))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
        assertEquals(1, gauge.get());
    }

    @Test
    public void shouldGiveServerErrorWhenDecryptedKeyDoesNotMatch() throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId(anyString())).willReturn(ENCRYPTION_KEY_ID);
        GenerateDataKeyResponse response =
                new GenerateDataKeyResponse(ENCRYPTION_KEY_ID, PLAIN_TEXT_KEY, CIPHER_TEXT_DATA_KEY);

        given(dataKeyGeneratorProvider.generateDataKey(eq(ENCRYPTION_KEY_ID), anyString())).willReturn(response);

        DecryptDataKeyResponse decryptResponse = new DecryptDataKeyResponse(ENCRYPTION_KEY_ID, PLAIN_TEXT_KEY);

        given(dataKeyDecryptionProvider.decryptDataKey(eq(ENCRYPTION_KEY_ID), eq(CIPHER_TEXT_DATA_KEY + "_CHANGE"), anyString()))
                .willReturn(decryptResponse);

        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.BAD);

        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
        assertEquals(1, gauge.get());
    }

    @Test
    public void shouldReturnStatusOkWhenAllChecksPass() throws Exception {

        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId(anyString())).willReturn(ENCRYPTION_KEY_ID);

        GenerateDataKeyResponse response =
                new GenerateDataKeyResponse(ENCRYPTION_KEY_ID, PLAIN_TEXT_KEY, CIPHER_TEXT_DATA_KEY);

        given(dataKeyGeneratorProvider.generateDataKey(eq(ENCRYPTION_KEY_ID), anyString())).willReturn(response);
        DecryptDataKeyResponse decryptResponse = new DecryptDataKeyResponse(ENCRYPTION_KEY_ID, PLAIN_TEXT_KEY);

        given(dataKeyDecryptionProvider.decryptDataKey(eq(ENCRYPTION_KEY_ID), eq(CIPHER_TEXT_DATA_KEY), anyString()))
                .willReturn(decryptResponse);

        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK);

        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isOk());
        assertEquals(0, gauge.get());
    }

}
