package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class TagServiceImpl implements TagService {

	@Override
	public Map<String, String> getResourceTags(UserInfo userInfo, String endpoint, String project,
											   String customTag) {
		Map<String, String> tags = new HashMap<>();
		tags.put("user_tag", userInfo.getName());
		tags.put("endpoint_tag", endpoint);
		tags.put("project_tag", project);
		Optional.ofNullable(customTag).ifPresent(t -> tags.put("custom_tag", t));
		return tags;
	}
}
