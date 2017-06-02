package com.epam.dlab.automation.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Networks {
    
    @JsonProperty("Bridge")
    private Bridge Bridge;

    public Bridge getBridge() {
        return Bridge;
    }

    public void setBridge(Bridge bridge) {
        this.Bridge = bridge;
    }

}
