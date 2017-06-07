package com.epam.dlab.automation.aws;

public enum AmazonInstanceState {
    RUNNING,
    TERMINATED;
	
    @Override
    public String toString() {
    	return super.toString().toLowerCase();
    }
}
