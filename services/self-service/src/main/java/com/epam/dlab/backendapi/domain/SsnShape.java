package com.epam.dlab.backendapi.domain;

import com.epam.dlab.dto.UserInstanceStatus;
import lombok.Builder;

import java.util.Collections;

public class SsnShape extends BaseShape {

    @Builder
    public SsnShape(String shape, UserInstanceStatus status) {
        super(shape, status, Collections.emptyMap());
    }
}
