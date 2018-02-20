package com.epam.dlab.automation.cloud.gcp;

public enum GcpInstanceState {
	PROVISIONING,
	STAGING,
	RUNNING,
	STOPPING,
	TERMINATED;

	@Override
	public String toString() {
		return super.toString().toUpperCase();
	}
}

