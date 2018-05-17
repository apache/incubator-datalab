package com.epam.dlab.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ResourceData {
	private ResourceType resourceType;
	private String resourceId;
	private String exploratoryName;
	private String computationalName;
}
