package com.epam.dlab.billing.gcp.service;

import com.epam.dlab.billing.gcp.dao.BillingDAO;
import com.epam.dlab.billing.gcp.documents.UserInstance;
import com.epam.dlab.billing.gcp.model.GcpBillingData;
import com.epam.dlab.billing.gcp.repository.BillingRepository;
import com.epam.dlab.billing.gcp.repository.ProjectRepository;
import com.epam.dlab.billing.gcp.repository.UserInstanceRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class BillingServiceImplTest {
    @Mock
    private BillingDAO billingDAO;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserInstanceRepository userInstanceRepository;
    @Mock
    private BillingRepository billingRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @InjectMocks
    private BillingServiceImpl billingService;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(billingService, "sbn", "CONF_SERVICE_BASE_NAME");
    }

    @Test
    public void updateBillingData() throws InterruptedException {
        when(userInstanceRepository.findAll()).thenReturn(getUserInstances());
        when(billingDAO.getBillingData()).thenReturn(getBillingData());

        billingService.updateBillingData();

        verify(userInstanceRepository).findAll();
        verify(userInstanceRepository, times(1)).findAll();
        verify(billingDAO).getBillingData();
        verify(billingDAO, times(1)).getBillingData();
        verify(projectRepository, times(1)).findAll();
        verify(billingRepository, times(1)).deleteByUsageDateRegex(anyString());
        verify(billingRepository, times(1)).insert(anyCollection());

        verifyNoMoreInteractions(billingDAO, userInstanceRepository, projectRepository);
    }

    private List<UserInstance> getUserInstances() {
        UserInstance userInstance1 = new UserInstance();
        userInstance1.setComputationalResources(Collections.emptyList());

        UserInstance userInstance2 = new UserInstance();
        userInstance2.setComputationalResources(Collections.emptyList());
        userInstance2.setExploratoryId("exploratoryIId");

        return Arrays.asList(userInstance1, userInstance1, userInstance2);
    }

    private List<GcpBillingData> getBillingData() {
        return Collections.singletonList(GcpBillingData.builder()
                .usageDateFrom(LocalDate.MIN)
                .usageDateTo(LocalDate.MAX)
                .product("product")
                .usageType("usageType")
                .cost(new BigDecimal(1))
                .currency("USD")
                .tag("exploratoryId")
                .build());
    }
}