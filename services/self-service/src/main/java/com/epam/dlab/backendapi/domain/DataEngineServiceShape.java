package com.epam.dlab.backendapi.domain;

import com.epam.dlab.backendapi.service.ShapeFormat;
import com.epam.dlab.dto.UserInstanceStatus;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;


@Slf4j
public class DataEngineServiceShape extends BaseShape implements ShapeFormat {
    private static final String DES_NAME_FORMAT = "Master: %s%sSlave:  %d x %s";
    private String slaveCount;
    private String slaveShape;

    @Builder
    public DataEngineServiceShape(String shape, UserInstanceStatus status, String slaveCount, String slaveShape,
                                  Map<String, String> tags) {
        super(shape, status, tags);
        this.slaveCount = slaveCount;
        this.slaveShape = slaveShape;
    }

    @Override
    public String format() {
        Integer count;
        try {
            count = Integer.valueOf(slaveCount);
        } catch (NumberFormatException e) {
            log.error("Cannot parse string {} to integer", slaveCount);
            return StringUtils.EMPTY;
        }
        return String.format(DES_NAME_FORMAT, shape, System.lineSeparator(), count - 1, slaveShape);
    }
}
