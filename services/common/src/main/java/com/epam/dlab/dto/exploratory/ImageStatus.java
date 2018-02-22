package com.epam.dlab.dto.exploratory;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum ImageStatus {
    CREATING,
    CREATED,
    FAILED;

    @JsonCreator
    public static ImageStatus fromValue(final String status) {
        return Arrays.stream(ImageStatus.values())
                .filter(s -> s.name().equalsIgnoreCase(status))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Wrong value for image status: %s", status)));
    }
}
