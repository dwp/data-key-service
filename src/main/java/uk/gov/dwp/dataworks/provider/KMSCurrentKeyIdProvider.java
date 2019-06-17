package uk.gov.dwp.dataworks.provider;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;

@Service
@Profile("KMS")
public class KMSCurrentKeyIdProvider implements CurrentKeyIdProvider {

    private final AWSSimpleSystemsManagement awsSimpleSystemsManagementClient;

    @Autowired
    public KMSCurrentKeyIdProvider(AWSSimpleSystemsManagement awsSimpleSystemsManagementClient) {
        this.awsSimpleSystemsManagementClient = awsSimpleSystemsManagementClient;
    }

    public String getKeyId() throws CurrentKeyIdException {
        try {
            GetParameterRequest request = new GetParameterRequest()
                    .withName("data-key-service.currentKeyId")
                    .withWithDecryption(false);
            GetParameterResult result = awsSimpleSystemsManagementClient.getParameter(request);
            return result.getParameter().getValue();
        }
        catch (Exception e) {
            throw new CurrentKeyIdException(e);
        }
    }
}
