package uk.gov.dwp.dataworks.provider;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdFailure;

@Service
@Profile("KMS")
public class KMSCurrentKeyIdProvider implements CurrentKeyIdProvider {
    private Logger logger = LoggerFactory.getLogger(KMSCurrentKeyIdProvider.class);

    public String getKeyId() throws CurrentKeyIdFailure {
        try {
            AWSSimpleSystemsManagement client = AWSSimpleSystemsManagementClientBuilder.defaultClient();
            GetParameterRequest request = new GetParameterRequest()
                    .withName("data-key-service.currentKeyId")
                    .withWithDecryption(false);
            GetParameterResult result = client.getParameter(request);
            return result.getParameter().getValue();
        }
        catch(Exception ex) {
            logger.error("Exception caught while communicating with parameter store", ex);
            throw new CurrentKeyIdFailure();
        }
    }

}
