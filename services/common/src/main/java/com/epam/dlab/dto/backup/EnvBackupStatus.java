package com.epam.dlab.dto.backup;

import java.util.Arrays;

public enum EnvBackupStatus {
	CREATING("N/A"), CREATED("N/A"), FAILED("N/A");

	private String message;

	EnvBackupStatus(String message) {
		this.message = message;
	}

	public EnvBackupStatus withErrorMessage(String message) {
		this.message = message;
		return this;
	}

	public String message() {
		return message;
	}

	public static EnvBackupStatus fromValue(String value) {
		return Arrays.stream(values())
				.filter(v -> v.name().equalsIgnoreCase(value))
				.findAny()
				.orElseThrow(() ->
						new IllegalArgumentException("Wrong value for EnvBackupStatus: " + value));
	}
}
