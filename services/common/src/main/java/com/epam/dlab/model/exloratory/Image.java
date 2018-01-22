package com.epam.dlab.model.exloratory;

import com.epam.dlab.dto.exploratory.ImageStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Image {
    private final String name;
    private final String description;
    private final ImageStatus status;
    private final String exploratoryId;
    private final String user;
}
