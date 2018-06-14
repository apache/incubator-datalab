package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.keyload.KeyLoadStatus;

public interface AccessKeyService {

	KeyLoadStatus getUserKeyStatus(String user);

	String uploadKey(UserInfo user, String keyContent, boolean isPrimaryUploading);

	String recoverEdge(UserInfo userInfo);

	String generateKey(UserInfo userInfo);

}
