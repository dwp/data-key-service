package uk.gov.dwp.dataworks.provider.aws;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;

@Service
@Profile({"KMS", "HSM"})
public class AwsCurrentKeyIdProvider implements CurrentKeyIdProvider {

    private final AWSSimpleSystemsManagement awsSimpleSystemsManagementClient;
    private final static Logger LOGGER = LoggerFactory.getLogger(AwsCurrentKeyIdProvider.class);

    @Autowired
    public AwsCurrentKeyIdProvider(AWSSimpleSystemsManagement awsSimpleSystemsManagementClient) {
        this.awsSimpleSystemsManagementClient = awsSimpleSystemsManagementClient;
    }

    public String getKeyId(String correlationId) throws CurrentKeyIdException {
        try {
            GetParameterRequest request = new GetParameterRequest()
                    .withName(masterkeyParameterName)
                    .withWithDecryption(false);
            GetParameterResult result = awsSimpleSystemsManagementClient.getParameter(request);
            return result.getParameter().getValue();
        } catch (RuntimeException e) {
            CurrentKeyIdException wrapper = new CurrentKeyIdException(correlationId);
            LOGGER.error(wrapper.getMessage(), e);
            throw wrapper;
        }
    }

    @Override
    public boolean canSeeDependencies() {
        return getKeyId("NOT_SET") != null;
    }

    @Value("${master.key.parameter.name:data_key_service.currentKeyId}")
    private String masterkeyParameterName;

}
