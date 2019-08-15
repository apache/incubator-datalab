package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;

import java.util.Map;

public interface TagService {
	Map<String, String> getResourceTags(UserInfo userInfo, String endpoint, String project, String customTag);
}
