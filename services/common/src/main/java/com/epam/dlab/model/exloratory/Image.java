package com.epam.dlab.model.exloratory;

import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.model.library.Library;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class Image {
	private final String name;
	private final String description;
	private final ImageStatus status;
	private final String exploratoryId;
	private final String user;
	private final String fullName;
	private final String externalName;
	private final String application;
	private final String dockerImage;
	private final List<Library> libraries;
	private final Map<String, List<Library>> computationalLibraries;
}
