package com.epam.dlab.backendapi.domain;

import com.epam.dlab.dto.UserInstanceStatus;
import lombok.Builder;

import java.util.Map;

public class ExploratoryShape extends BaseShape {

    @Builder
    public ExploratoryShape(String shape, UserInstanceStatus status, Map<String, String> tags) {
        super(shape, status, tags);
    }
}
