package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.keyload.KeyLoadStatus;

public interface AccessKeyService {
	KeyLoadStatus getUserKeyStatus(String user);

	String uploadKey(UserInfo user, String keyContent);

	String recoverEdge(UserInfo userInfo);

}
