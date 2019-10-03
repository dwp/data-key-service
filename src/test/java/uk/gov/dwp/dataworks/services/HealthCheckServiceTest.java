package uk.gov.dwp.dataworks.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dwp.dataworks.controller.HealthCheckController;
import uk.gov.dwp.dataworks.dto.HealthCheckResponse;
import uk.gov.dwp.dataworks.provider.CurrentKeyIdProvider;
import uk.gov.dwp.dataworks.provider.DataKeyDecryptionProvider;
import uk.gov.dwp.dataworks.provider.DataKeyGeneratorProvider;
import uk.gov.dwp.dataworks.service.HealthCheckService;

import static uk.gov.dwp.dataworks.dto.HealthCheckResponse.Health.OK;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"healthcheck.interval=1000", "scheduling.enabled=true"})
public class HealthCheckServiceTest {

    private final ResponseEntity<HealthCheckResponse> mockHealthOkResponse = org.springframework.http.ResponseEntity.ok(new HealthCheckResponse(OK, OK, OK, OK, OK));

    @Autowired
    private HealthCheckService healthCheckService;

    @MockBean
    private HealthCheckController healthCheckController;

    @MockBean
    private DataKeyGeneratorProvider dataKeyGeneratorProvider;

    @MockBean
    private CurrentKeyIdProvider currentKeyIdProvider;

    @MockBean
    private DataKeyDecryptionProvider dataKeyDecryptionProvider;

    @Test
    public void Should_Run_Healthcheck_Initially_After_One_Second() throws InterruptedException  {
        Mockito.when(healthCheckController.healthCheck()).thenReturn(mockHealthOkResponse);
        Thread.sleep(1000);
        Mockito.verify(healthCheckController, Mockito.times(1)).healthCheck();
    }

    @Test
    public void Should_Run_Healthcheck_At_Specified_Interval() throws InterruptedException {
        Mockito.when(healthCheckController.healthCheck()).thenReturn(mockHealthOkResponse);
        Thread.sleep(3000);
        Mockito.verify(healthCheckController, Mockito.times(3)).healthCheck();
    }
}
