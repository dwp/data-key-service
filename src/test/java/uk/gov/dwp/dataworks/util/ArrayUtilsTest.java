package uk.gov.dwp.dataworks.util;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ArrayUtilsTest {

    @Test
    public void reverseTest(){
        // Set up some test data
        String forwardString =  "Hello, there. 68";
        String backwardString = "86 .ereht ,olleH";
        byte[] forwardBytes = forwardString.getBytes(StandardCharsets.US_ASCII);
        byte[] backwardBytes = backwardString.getBytes(StandardCharsets.US_ASCII);

        // Reversing the backward bytes should make it the same as the forward bytes
        byte[] backwardBytesReverse = Arrays.copyOf(backwardBytes, backwardBytes.length);
        ArrayUtils.reverse(backwardBytesReverse);
        Assert.assertArrayEquals(forwardBytes, backwardBytesReverse);
    }

    @Test
    public void handlesNull(){
        // Reversing null does not throw error
        ArrayUtils.reverse(null);
    }

    @Test
    public void handledEmptyArray(){
        byte[] emptyArray = new byte[0];
        ArrayUtils.reverse(emptyArray);
    }
}
