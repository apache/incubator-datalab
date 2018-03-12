package com.epam.dlab.automation.model;

public class ImageDto {

	private String name;
	private String description;
	private String application;
	private String fullName;
	private String status;

	public ImageDto() {
	}

	public ImageDto(String name, String description, String application, String fullName, String status) {

		this.name = name;
		this.description = description;
		this.application = application;
		this.fullName = fullName;
		this.status = status;
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

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "ImageDto{" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				", application='" + application + '\'' +
				", fullName='" + fullName + '\'' +
				", status='" + status + '\'' +
				'}';
	}
}
