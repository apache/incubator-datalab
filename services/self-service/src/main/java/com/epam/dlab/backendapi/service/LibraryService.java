package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.LibInfoRecord;
import com.epam.dlab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import org.bson.Document;

import java.util.List;

public interface LibraryService {
	List<Document> getLibs(String user, String exploratoryName, String computationalName);

	List<LibInfoRecord> getLibInfo(String user, String exploratoryName);

	LibraryInstallDTO generateLibraryInstallDTO(UserInfo userInfo, LibInstallFormDTO formDTO);

	LibraryInstallDTO prepareExploratoryLibInstallation(String username, LibInstallFormDTO formDTO,
														LibraryInstallDTO dto);

	LibraryInstallDTO prepareComputationalLibInstallation(String username, LibInstallFormDTO formDTO,
														  LibraryInstallDTO dto);
}
