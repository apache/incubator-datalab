package com.epam.dlab.automation.docker;

public enum Status {
    
    EXITED_0("0"), EXITED_1("1");
    private String value;

    Status(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
