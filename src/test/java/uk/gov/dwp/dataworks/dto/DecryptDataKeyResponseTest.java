package uk.gov.dwp.dataworks.dto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DecryptDataKeyResponseTest {

    @Test
    public void testThatcorrelationIdIsNotInEquality()  {
        DecryptDataKeyResponse response1 = new DecryptDataKeyResponse("key", "value");
        DecryptDataKeyResponse response2 = new DecryptDataKeyResponse("key", "value");

        assertEquals(response1, response2);
        assertEquals(response1.toString(), response2.toString());
        assertEquals(response1.hashCode(), response2.hashCode());

        response1.setCorrelationId("correlation-1");
        response2.setCorrelationId("correlation-2");

        assertEquals(response1, response2);
        assertEquals(response1.toString(), response2.toString());
        assertEquals(response1.hashCode(), response2.hashCode());

        DecryptDataKeyResponse response3 = new DecryptDataKeyResponse("keyX", "valueX");
        response1.setCorrelationId("correlation-X");
        response3.setCorrelationId(response1.getCorrelationId());

        assertNotEquals(response1, response3);
        assertNotEquals(response1.toString(), response3.toString());
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

}
