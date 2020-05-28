package uk.gov.dwp.dataworks.helpers;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.dwp.dataworks.provider.hsm.CryptoImplementationSupplier;

@Configuration
@Profile("UnitTest")
public class ProviderUnitTestConfiguration {

    @Bean
    public CryptoImplementationSupplier keyGeneratorSupplier() {
        return Mockito.mock(CryptoImplementationSupplier.class);
    }

    @Bean
    public AWSSimpleSystemsManagement awsSimpleSystemsManagement() {
        return Mockito.mock(AWSSimpleSystemsManagement.class);
    }

    @Bean
    public AWSKMS awsKms() {
        return Mockito.mock(AWSKMS.class);
    }

    @Bean
    public AmazonS3 amazonS3() {
        return Mockito.mock(AmazonS3.class);
    }

}
