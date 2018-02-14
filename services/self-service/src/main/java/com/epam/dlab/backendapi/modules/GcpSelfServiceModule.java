package com.epam.dlab.backendapi.modules;

import com.epam.dlab.auth.SecurityFactory;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthenticator;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.gcp.GcpKeyDao;
import com.epam.dlab.backendapi.resources.callback.gcp.EdgeCallbackGcp;
import com.epam.dlab.backendapi.resources.callback.gcp.KeyUploaderCallbackGcp;
import com.epam.dlab.backendapi.resources.gcp.ComputationalResourceGcp;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.backendapi.service.InfrastructureTemplatesService;
import com.epam.dlab.backendapi.service.gcp.GcpInfrastructureInfoService;
import com.epam.dlab.backendapi.service.gcp.GcpInfrastructureTemplatesService;
import com.epam.dlab.cloud.CloudModule;
import com.google.inject.Injector;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.setup.Environment;

public class GcpSelfServiceModule extends CloudModule {
    @Override
    public void init(Environment environment, Injector injector) {

        environment.jersey().register(injector.getInstance(EdgeCallbackGcp.class));
        environment.jersey().register(injector.getInstance(KeyUploaderCallbackGcp.class));
        environment.jersey().register(injector.getInstance(ComputationalResourceGcp.class));

        injector.getInstance(SecurityFactory.class).configure(injector, environment,
                SelfServiceSecurityAuthenticator.class, injector.getInstance(Authorizer.class));

    }

    @Override
    protected void configure() {
        bind((KeyDAO.class)).to(GcpKeyDao.class);
        bind(InfrastructureInfoService.class).to(GcpInfrastructureInfoService.class);
        bind(InfrastructureTemplatesService.class).to(GcpInfrastructureTemplatesService.class);
    }
}
