package com.epam.dlab.billing.gcp.service;

import com.epam.dlab.billing.gcp.dao.BillingDAO;
import com.epam.dlab.billing.gcp.model.GcpBillingData;
import com.epam.dlab.billing.gcp.repository.BillingRepository;
import com.epam.dlab.billing.gcp.service.impl.BillingServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class BillingServiceImplTest {
    @Mock
    private BillingDAO billingDAO;
    @Mock
    private BillingRepository billingRepository;
    @InjectMocks
    private BillingServiceImpl billingService;

    @Test
    public void updateBillingData() throws InterruptedException {
        when(billingDAO.getBillingData()).thenReturn(getBillingData());

        billingService.updateBillingData();

        verify(billingDAO).getBillingData();
        verify(billingRepository).deleteByUsageDateRegex(anyString());
        verify(billingRepository).insert(anyCollection());

        verifyNoMoreInteractions(billingDAO);
    }

    private List<GcpBillingData> getBillingData() {
        return Collections.singletonList(GcpBillingData.builder()
                .usageDate(LocalDate.MIN.toString())
                .usageDateFrom(LocalDate.MIN)
                .usageDateTo(LocalDate.MAX)
                .product("product")
                .usageType("usageType")
                .cost(1d)
                .currency("USD")
                .tag("exploratoryId")
                .build());
    }
}