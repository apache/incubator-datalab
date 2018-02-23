package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;

public interface GitCredentialService {
	void updateGitCredentials(UserInfo userInfo, ExploratoryGitCredsDTO dto);

	ExploratoryGitCredsDTO getGitCredentials(String user);
}
