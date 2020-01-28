package uk.gov.dwp.dataworks.dto;

import static org.junit.Assert.*;
import org.junit.Test;

public class DecryptDataKeyResponseTest {

    @Test
    public void testThatDksCorrelationIdIsNotInEquality()  {
        DecryptDataKeyResponse response1 = new DecryptDataKeyResponse("key", "value");
        DecryptDataKeyResponse response2 = new DecryptDataKeyResponse("key", "value");

        assertEquals(response1, response2);
        assertEquals(response1.toString(), response2.toString());
        assertEquals(response1.hashCode(), response2.hashCode());

        response1.setDksCorrelationId("correlation-1");
        response2.setDksCorrelationId("correlation-2");

        assertEquals(response1, response2);
        assertEquals(response1.toString(), response2.toString());
        assertEquals(response1.hashCode(), response2.hashCode());

        DecryptDataKeyResponse response3 = new DecryptDataKeyResponse("keyX", "valueX");
        response1.setDksCorrelationId("correlation-X");
        response3.setDksCorrelationId(response1.getDksCorrelationId());

        assertNotEquals(response1, response3);
        assertNotEquals(response1.toString(), response3.toString());
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

}
