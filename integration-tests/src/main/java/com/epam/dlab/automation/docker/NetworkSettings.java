package com.epam.dlab.automation.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NetworkSettings {

    @JsonProperty
    private Networks Networks;

    public Networks getNetworks() {
        return Networks;
    }

    public void setNetworks(Networks networks) {
        this.Networks = networks;
    }
}
