package uk.gov.dwp.dataworks.provider.standalone;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.dwp.dataworks.errors.CurrentKeyIdException;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;

@Service
@Profile("STANDALONE")
public class StandaloneCurrentKeyIdProvider implements CurrentKeyIdProvider {

    @Override
    public String getKeyId() throws CurrentKeyIdException {
        return "STANDALONE";
    }

    @Override
    public boolean canSeeDependencies() {
        return true;
    }

}

