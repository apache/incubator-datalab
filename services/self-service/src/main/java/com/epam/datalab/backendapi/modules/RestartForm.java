package com.epam.datalab.backendapi.modules;

import lombok.Data;

@Data
public class RestartForm {

    private boolean billing;
    private boolean provserv;
    private boolean ui;
    private String endpoint;
}
