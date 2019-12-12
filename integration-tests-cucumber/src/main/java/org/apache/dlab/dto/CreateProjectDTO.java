package org.apache.dlab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProjectDTO {
    private String name;
    private Set<String> groups;
    private Set<String> endpoints;
    private String key;
    private String tag;
}

