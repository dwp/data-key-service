package uk.gov.dwp.dataworks.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.dwp.dataworks.dto.DecryptDataKeyResponse;
import uk.gov.dwp.dataworks.dto.GenerateDataKeyResponse;
import uk.gov.dwp.dataworks.dto.HealthCheckResponse;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

import java.util.Objects;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ActiveProfiles({"IntegrationTest", "INSECURE"})
@AutoConfigureMockMvc
public class HealthCheckIntegrationTests {

    @Autowired
    private CacheManager cacheManager;

    @Before
    public void setup() {
        Mockito.reset(currentKeyIdProvider, dataKeyGeneratorProvider, dataKeyDecryptionProvider);
        for (String name : cacheManager.getCacheNames()) {
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CurrentKeyIdProvider currentKeyIdProvider;

    @Autowired
    private DataKeyGeneratorProvider dataKeyGeneratorProvider;

    @Autowired
    private DataKeyDecryptionProvider dataKeyDecryptionProvider;

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
    }

    @Test
    public void shouldGiveServerErrorWhenMasterKeyCheckFails() throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId()).willThrow(RuntimeException.class);

        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD);
        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void shouldGiveServerErrorWhenMasterKeyCheckGivesEmptyKey() throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId()).willReturn("");
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD);
        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void shouldGiveServerErrorWhenGenerateKeyFails() throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId()).willReturn(ENCRYPTION_KEY_ID);
        given(dataKeyGeneratorProvider.generateDataKey(ENCRYPTION_KEY_ID)).willThrow(RuntimeException.class);

        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD,
                HealthCheckResponse.Health.BAD);

        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void shouldGiveServerErrorWhenDecryptionFails() throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId()).willReturn(ENCRYPTION_KEY_ID);
        GenerateDataKeyResponse response =
                new GenerateDataKeyResponse(ENCRYPTION_KEY_ID, PLAIN_TEXT_KEY, CIPHER_TEXT_DATA_KEY);

        given(dataKeyGeneratorProvider.generateDataKey(ENCRYPTION_KEY_ID)).willReturn(response);

        given(dataKeyDecryptionProvider.decryptDataKey(ENCRYPTION_KEY_ID, CIPHER_TEXT_DATA_KEY))
                .willThrow(RuntimeException.class);

        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.BAD);

        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void shouldGiveServerErrorWhenDecryptedKeyDoesntMatch() throws Exception {
        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId()).willReturn(ENCRYPTION_KEY_ID);
        GenerateDataKeyResponse response =
                new GenerateDataKeyResponse(ENCRYPTION_KEY_ID, PLAIN_TEXT_KEY, CIPHER_TEXT_DATA_KEY);

        given(dataKeyGeneratorProvider.generateDataKey(ENCRYPTION_KEY_ID)).willReturn(response);

        DecryptDataKeyResponse decryptResponse = new DecryptDataKeyResponse(ENCRYPTION_KEY_ID, PLAIN_TEXT_KEY);

        given(dataKeyDecryptionProvider.decryptDataKey(ENCRYPTION_KEY_ID, CIPHER_TEXT_DATA_KEY + "_CHANGE"))
                .willReturn(decryptResponse);

        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.BAD);

        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void shouldReturnStatusOkWhenAllChecksPass() throws Exception {

        given(currentKeyIdProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyGeneratorProvider.canSeeDependencies()).willReturn(true);
        given(dataKeyDecryptionProvider.canSeeDependencies()).willReturn(true);
        given(currentKeyIdProvider.getKeyId()).willReturn(ENCRYPTION_KEY_ID);

        GenerateDataKeyResponse response =
                new GenerateDataKeyResponse(ENCRYPTION_KEY_ID, PLAIN_TEXT_KEY, CIPHER_TEXT_DATA_KEY);

        given(dataKeyGeneratorProvider.generateDataKey(ENCRYPTION_KEY_ID)).willReturn(response);
        DecryptDataKeyResponse decryptResponse = new DecryptDataKeyResponse(ENCRYPTION_KEY_ID, PLAIN_TEXT_KEY);

        given(dataKeyDecryptionProvider.decryptDataKey(ENCRYPTION_KEY_ID, CIPHER_TEXT_DATA_KEY))
                .willReturn(decryptResponse);

        HealthCheckResponse healthCheckResponse = new HealthCheckResponse(HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK,
                HealthCheckResponse.Health.OK);

        mockMvc.perform(get(HEALTHCHECK_ENDPOINT))
                .andExpect(content().json(new ObjectMapper().writeValueAsString(healthCheckResponse)))
                .andExpect(status().isOk());
    }

}
