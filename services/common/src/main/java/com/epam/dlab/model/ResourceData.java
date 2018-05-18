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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (resourceType == ResourceType.EDGE) {
			return sb.append(resourceType.toString()).toString();
		} else if (resourceType == ResourceType.EXPLORATORY) {
			return sb.append(resourceType.toString()).append(" ").append(exploratoryName).toString();
		} else if (resourceType == ResourceType.COMPUTATIONAL) {
			return sb.append(resourceType.toString()).append(" ").append(computationalName)
					.append(" affiliated with exploratory ").append(exploratoryName).toString();
		} else return "";
	}
}
