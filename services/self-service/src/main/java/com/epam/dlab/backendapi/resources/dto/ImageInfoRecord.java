package com.epam.dlab.backendapi.resources.dto;

import lombok.Data;

@Data
public class ImageInfoRecord {

    private final String name;
    private final String description;
    private final String application;
    private final String fullName;
}
