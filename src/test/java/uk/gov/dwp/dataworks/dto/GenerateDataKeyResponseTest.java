package uk.gov.dwp.dataworks.dto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class GenerateDataKeyResponseTest {

    @Test
    public void testThatDksCorrelationIdIsNotInEquality()  {
        GenerateDataKeyResponse response1 = new GenerateDataKeyResponse("key", "plain", "cypher");
        GenerateDataKeyResponse response2 = new GenerateDataKeyResponse("key", "plain", "cypher");

        assertEquals(response1, response2);
        assertEquals(response1.toString(), response2.toString());
        assertEquals(response1.hashCode(), response2.hashCode());

        response1.setDksCorrelationId("correlation-1");
        response2.setDksCorrelationId("correlation-2");

        assertEquals(response1, response2);
        assertEquals(response1.toString(), response2.toString());
        assertEquals(response1.hashCode(), response2.hashCode());

        GenerateDataKeyResponse response3 = new GenerateDataKeyResponse("keyX", "valueX", "cypherX");
        response1.setDksCorrelationId("correlation-X");
        response3.setDksCorrelationId(response1.getDksCorrelationId());

        assertNotEquals(response1, response3);
        assertNotEquals(response1.toString(), response3.toString());
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

}
