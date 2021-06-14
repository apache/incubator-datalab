package com.epam.datalab.properties;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestartAnswer {

    private boolean billingSuccess;
    private boolean provservSuccess;
    private String endpoint;
}
