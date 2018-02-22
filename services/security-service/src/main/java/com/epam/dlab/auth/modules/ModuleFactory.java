package com.epam.dlab.auth.modules;

import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.cloud.CloudModule;

public class ModuleFactory {

	private ModuleFactory() {
	}

	public static CloudModule getCloudProviderModule(SecurityServiceConfiguration configuration) {
		switch (configuration.getCloudProvider()) {
			case AWS:
				return new AwsSecurityServiceModule(configuration);
			case AZURE:
				return new AzureSecurityServiceModule(configuration);
			case GCP:
				return new GcpSecurityServiceModule(configuration);
			default:
				throw new UnsupportedOperationException(
						String.format("Unsupported cloud provider %s", configuration.getCloudProvider()));
		}
	}
}
