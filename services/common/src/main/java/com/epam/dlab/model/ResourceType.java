package com.epam.dlab.model;

public enum ResourceType {
	COMPUTATIONAL("computational_resource"),
	EDGE("edge_node"),
	EXPLORATORY("exploratory");

	private String name;

	ResourceType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
