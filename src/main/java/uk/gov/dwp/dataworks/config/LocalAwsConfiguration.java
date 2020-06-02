package uk.gov.dwp.dataworks.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("LocalAWS")
class LocalAwsConfiguration {

    @Bean
    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
            .withClientConfiguration(new ClientConfiguration().withProtocol(Protocol.HTTP))
            .withPathStyleAccessEnabled(true)
            .disableChunkedEncoding()
            .build();
    }

    @Value("${aws.region:eu-west-2}")
    private String region;

    @Value("${s3.service.endpoint:http://s3-dummy:4572}")
    private String serviceEndpoint;
}
