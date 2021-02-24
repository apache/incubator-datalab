package com.epam.datalab.properties;

import lombok.Data;

@Data
public class RestartForm {

    private boolean billing;
    private boolean provserv;
    private boolean ui;
    private String endpoint;
}
