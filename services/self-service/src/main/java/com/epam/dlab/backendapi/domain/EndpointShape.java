package com.epam.dlab.backendapi.domain;

import com.epam.dlab.dto.UserInstanceStatus;
import lombok.Builder;

import java.util.Collections;

public class EndpointShape extends BaseShape {

    @Builder
    public EndpointShape(String shape, UserInstanceStatus status) {
        super(shape, status, Collections.emptyMap());
    }
}
