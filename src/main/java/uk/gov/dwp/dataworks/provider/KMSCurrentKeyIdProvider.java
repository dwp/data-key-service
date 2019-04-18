package uk.gov.dwp.dataworks.provider;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import org.springframework.stereotype.Service;

@Service
public class KMSCurrentKeyIdProvider implements CurrentKeyIdProvider {

    public String getKeyId() {
        AWSSimpleSystemsManagement client= AWSSimpleSystemsManagementClientBuilder.defaultClient();
        GetParameterRequest request = new GetParameterRequest()
                .withName("data-key-service.currentKeyId")
                .withWithDecryption(false);
        GetParameterResult result = client.getParameter(request);
        return result.getParameter().getValue();
    }

}
