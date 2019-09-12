package uk.gov.dwp.dataworks.provider.standalone;

import org.junit.Assert;
import org.junit.Test;

public class StandaloneCurrentKeyIdProviderTest {
    private final StandaloneCurrentKeyIdProvider providerToTest = new StandaloneCurrentKeyIdProvider();

    @Test
    public void getKeyId() {
        Assert.assertEquals("Must be STANDALONE", "STANDALONE", providerToTest.getKeyId());
    }

    @Test
    public void canSeeDependencies() {
        Assert.assertTrue(providerToTest.canSeeDependencies());
    }
}
