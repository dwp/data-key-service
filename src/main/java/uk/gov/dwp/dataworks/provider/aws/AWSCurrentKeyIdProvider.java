package uk.gov.dwp.dataworks.provider.aws;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;

@Service
@Profile({"KMS", "HSM"})
public class AWSCurrentKeyIdProvider implements CurrentKeyIdProvider {

    private final AWSSimpleSystemsManagement awsSimpleSystemsManagementClient;
    private final static Logger LOGGER = LoggerFactory.getLogger(AWSCurrentKeyIdProvider.class);

    @Autowired
    public AWSCurrentKeyIdProvider(AWSSimpleSystemsManagement awsSimpleSystemsManagementClient) {
        this.awsSimpleSystemsManagementClient = awsSimpleSystemsManagementClient;
    }

    public String getKeyId() throws CurrentKeyIdException {
        try {
//            GetParameterRequest request = new GetParameterRequest()
//                    .withName("data_key_service.currentKeyId")
//                    .withWithDecryption(false);
//            GetParameterResult result = awsSimpleSystemsManagementClient.getParameter(request);
//            result.getParameter().getValue();
            return "cloudhsm:7,14";
        }
        catch (RuntimeException e) {
            LOGGER.error("Failed to retrieve the current key id.", e);
            throw new CurrentKeyIdException();
        }
    }

    @Override
    public boolean canSeeDependencies() {
        return awsSimpleSystemsManagementClient != null;
    }
}
