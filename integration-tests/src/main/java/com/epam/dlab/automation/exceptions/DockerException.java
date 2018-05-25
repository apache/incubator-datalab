package com.epam.dlab.automation.exceptions;

public class DockerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DockerException(String message) {
		super(message);
	}
}
