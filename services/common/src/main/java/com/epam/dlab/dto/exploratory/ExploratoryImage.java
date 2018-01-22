package com.epam.dlab.dto.exploratory;

import com.epam.dlab.dto.ResourceSysBaseDTO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
public class ExploratoryImage extends ResourceSysBaseDTO<ExploratoryImage> {

    private String name;
    private String description;
    private ImageStatus status;
    private String exploratoryId;

    public ExploratoryImage(String name, String description, ImageStatus status, String exploratoryId, String user) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.exploratoryId = exploratoryId;
        this.setEdgeUserName(user);
    }
}
