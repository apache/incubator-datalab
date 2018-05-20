package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyStatusDTO;
import com.epam.dlab.model.ResourceData;

public interface ReuploadKeyService {

	String reuploadKey(UserInfo user, String keyContent);

	void waitForRunningStatus(UserInfo userInfo, ResourceData resourceData, long seconds);

	void reuploadKeyAction(UserInfo userInfo, ResourceData resourceData);

	void processReuploadKeyResponse(ReuploadKeyStatusDTO dto);
}
