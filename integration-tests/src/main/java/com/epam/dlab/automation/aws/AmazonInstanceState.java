package com.epam.dlab.automation.aws;

public enum AmazonInstanceState {
    
    RUNNING("running"), TERMINATED("terminated");
    private String value;

    AmazonInstanceState(String value) {
        this.value = value;

    }

    public String value() {
        return value;
    }
}
