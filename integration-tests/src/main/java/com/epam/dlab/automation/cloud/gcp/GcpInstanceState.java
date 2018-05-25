package com.epam.dlab.automation.cloud.gcp;

public enum GcpInstanceState {
	STARTING,
	RUNNING,
	TERMINATED,
	STOPPED;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}

