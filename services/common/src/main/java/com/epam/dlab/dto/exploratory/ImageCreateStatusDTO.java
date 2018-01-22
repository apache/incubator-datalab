package com.epam.dlab.dto.exploratory;

import com.epam.dlab.dto.StatusBaseDTO;
import lombok.Data;

@Data
public class ImageCreateStatusDTO extends StatusBaseDTO<ImageCreateStatusDTO> {
    private final String name;
    private final String description;
    private final String status;
    private final String errorMassage;
}
