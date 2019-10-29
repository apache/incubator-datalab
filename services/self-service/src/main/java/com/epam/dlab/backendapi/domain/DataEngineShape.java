package com.epam.dlab.backendapi.domain;

import com.epam.dlab.backendapi.service.ShapeFormat;
import com.epam.dlab.dto.UserInstanceStatus;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Slf4j
public class DataEngineShape extends BaseShape implements ShapeFormat {
    private static final String DE_NAME_FORMAT = "%d x %s";
    private String slaveCount;


    @Builder
    public DataEngineShape(String shape, UserInstanceStatus status, String slaveCount, Map<String, String> tags) {
        super(shape, status, tags);
        this.slaveCount = slaveCount;
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
        return String.format(DE_NAME_FORMAT, count, shape);
    }
}
