package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.model.ResourceType;
import com.epam.dlab.model.exloratory.Image;
import com.epam.dlab.model.library.Library;

import java.util.List;
import java.util.Optional;

public interface ImageExploratoryDao {

	boolean exist(String user, String name);

	void save(Image image);

	void updateImageFields(Image image);

	List<ImageInfoRecord> getImages(String user, ImageStatus status, String dockerImage);

	Optional<ImageInfoRecord> getImage(String user, String name);

	List<Library> getLibraries(String user, String imageFullName, ResourceType resourceType, LibStatus status);
}
