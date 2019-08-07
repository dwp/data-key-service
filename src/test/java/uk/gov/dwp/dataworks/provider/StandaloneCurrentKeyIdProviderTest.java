package uk.gov.dwp.dataworks.provider;

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
        Assert.assertEquals(true, providerToTest.canSeeDependencies());
    }
}
