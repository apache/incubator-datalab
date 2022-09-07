package com.epam.datalab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SharedWith {
    private Set<String> users=new HashSet<>();
    private Set<String> groups=new HashSet<>();
}
