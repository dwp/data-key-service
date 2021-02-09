package uk.gov.dwp.dataworks.integration;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.dwp.dataworks.config.InsecureConfiguration;
import uk.gov.dwp.dataworks.controller.PingController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { PingController.class, InsecureConfiguration.class })
@ActiveProfiles({"INSECURE"})
@AutoConfigureMockMvc
public class PingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AmazonS3 amazonS3;

    @Test
    public void shouldSucceedWhenRequestingPing() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
