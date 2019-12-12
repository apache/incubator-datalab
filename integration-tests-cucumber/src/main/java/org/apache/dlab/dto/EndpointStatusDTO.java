package org.apache.dlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndpointStatusDTO {
    private String name;
    private Status status;


    public enum Status {
        CREATING,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        TERMINATING,
        TERMINATED,
        FAILED
    }
}
