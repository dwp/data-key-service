package uk.gov.dwp.dataworks.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("STANDALONE")
public class StandaloneCurrentKeyIdProvider implements CurrentKeyIdProvider {

    @Autowired
    public StandaloneCurrentKeyIdProvider(){

    }

    public String getKeyId() {
        return "STANDALONE";
    }

    @Override
    public boolean canSeeDependencies() {
        return true;
    }
}

