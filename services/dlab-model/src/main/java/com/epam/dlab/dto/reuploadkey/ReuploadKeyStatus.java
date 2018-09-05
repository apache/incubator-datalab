package com.epam.dlab.dto.reuploadkey;

import java.util.Arrays;

public enum ReuploadKeyStatus {

	COMPLETED("N/A"), FAILED("N/A");

	private String message;

	ReuploadKeyStatus(String message) {
		this.message = message;
	}

	public ReuploadKeyStatus withErrorMessage(String message) {
		this.message = message;
		return this;
	}

	public String message() {
		return message;
	}

	public static ReuploadKeyStatus fromValue(String value) {
		return Arrays.stream(values())
				.filter(v -> v.name().equalsIgnoreCase(value))
				.findAny()
				.orElseThrow(() ->
						new IllegalArgumentException("Wrong value for ReuploadKeyStatus: " + value));
	}
}
