package com.epam.dlab.automation.cloud.gcp;

public enum GcpInstanceState {
	PROVISIONING,
	STAGING,
	STARTING,
	RUNNING,
	STOPPING,
	TERMINATED;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}

