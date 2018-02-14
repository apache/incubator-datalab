package com.epam.dlab.model.exloratory;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Exploratory {
    private final String name;
    private final String dockerImage;
    private final String version;
    private final String templateName;
    private final String shape;
    private final String imageName;
}
