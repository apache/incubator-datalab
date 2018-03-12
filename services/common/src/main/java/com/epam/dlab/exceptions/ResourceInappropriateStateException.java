package com.epam.dlab.exceptions;

public class ResourceInappropriateStateException extends ResourceConflictException {
	public ResourceInappropriateStateException(String message) {
		super(message);
	}
}
