package com.epam.dlab.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExploratoryImageDto {

	@JsonProperty("exploratory_name")
	private String notebookName;
	private String name;
	private String description;

	public ExploratoryImageDto() {
	}

	public ExploratoryImageDto(String notebookName, String name, String description) {
		this.notebookName = notebookName;
		this.name = name;
		this.description = description;
	}

	public String getNotebookName() {
		return notebookName;
	}

	public void setNotebookName(String notebookName) {
		this.notebookName = notebookName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
