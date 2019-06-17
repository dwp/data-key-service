package uk.gov.dwp.dataworks.helpers;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;

@Configuration
@Profile("IntegrationTest")
public class ProviderIntegrationTestConfiguration {

    @Bean
    public CurrentKeyIdProvider currentKeyIdProvider() {
        return Mockito.mock(CurrentKeyIdProvider.class);
    }

    @Bean
    public DataKeyDecryptionProvider dataKeyDecryptionProvider() {
        return Mockito.mock(DataKeyDecryptionProvider.class);
    }

    @Bean
    public DataKeyGeneratorProvider dataKeyGeneratorProvider() {
        return Mockito.mock(DataKeyGeneratorProvider.class);
    }

}
