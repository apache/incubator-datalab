package com.epam.dlab.backendapi.domain;

import com.epam.dlab.backendapi.service.ShapeFormat;
import com.epam.dlab.dto.UserInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseShape implements ShapeFormat {
    protected String shape;
    protected UserInstanceStatus status;
    protected Map<String, String> tags;

    @Override
    public String format() {
        return shape;
    }
}
