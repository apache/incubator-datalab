package com.epam.dlab.automation.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HostConfig {
    
    @JsonProperty("NetworkMode")
    private String NetworkMode;

    public String getNetworkMode() {
        return NetworkMode;
    }

    public void setNetworkMode(String networkMode) {
        this.NetworkMode = networkMode;
    }

}
