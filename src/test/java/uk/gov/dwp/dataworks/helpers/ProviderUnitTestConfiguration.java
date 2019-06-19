package uk.gov.dwp.dataworks.helpers;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("UnitTest")
public class ProviderUnitTestConfiguration {

    @Bean
    public AWSSimpleSystemsManagement awsSimpleSystemsManagement() {
        return Mockito.mock(AWSSimpleSystemsManagement.class);
    }

    @Bean
    public AWSKMS awsKms() {
        return Mockito.mock(AWSKMS.class);
    }
}
