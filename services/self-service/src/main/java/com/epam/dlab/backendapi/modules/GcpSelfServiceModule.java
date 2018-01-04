package com.epam.dlab.backendapi.modules;

import com.epam.dlab.auth.SecurityFactory;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthenticator;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.gcp.GcpKeyDao;
import com.epam.dlab.backendapi.domain.gcp.BillingSchedulerManagerGcp;
import com.epam.dlab.backendapi.resources.callback.gcp.EdgeCallbackGcp;
import com.epam.dlab.backendapi.resources.callback.gcp.KeyUploaderCallbackGcp;
import com.epam.dlab.backendapi.resources.gcp.BillingResourceGcp;
import com.epam.dlab.backendapi.resources.gcp.ComputationalResourceGcp;
import com.epam.dlab.backendapi.service.BillingService;
import com.epam.dlab.backendapi.service.GcpBillingService;
import com.epam.dlab.backendapi.service.GcpInfrastructureInfoService;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.cloud.CloudModule;
import com.google.inject.Injector;
import io.dropwizard.setup.Environment;

public class GcpSelfServiceModule extends CloudModule {
    @Override
    public void init(Environment environment, Injector injector) {

        environment.jersey().register(injector.getInstance(EdgeCallbackGcp.class));
        environment.jersey().register(injector.getInstance(KeyUploaderCallbackGcp.class));
        environment.jersey().register(injector.getInstance(ComputationalResourceGcp.class));
        environment.jersey().register(injector.getInstance(BillingResourceGcp.class));
        environment.lifecycle().manage(injector.getInstance(BillingSchedulerManagerGcp.class));

        injector.getInstance(SecurityFactory.class).configure(injector, environment,
                SelfServiceSecurityAuthenticator.class, (p, r) -> true);

    }

    @Override
    protected void configure() {
        bind(BillingService.class).to(GcpBillingService.class);
        bind((KeyDAO.class)).to(GcpKeyDao.class);
        bind(InfrastructureInfoService.class).to(GcpInfrastructureInfoService.class);
    }
}
