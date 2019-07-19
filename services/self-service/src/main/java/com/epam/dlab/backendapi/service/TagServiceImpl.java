package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EndpointDAO;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class TagServiceImpl implements TagService {
	private final EndpointDAO endpointDAO;
	private final ProjectDAO projectDAO;

	@Inject
	public TagServiceImpl(EndpointDAO endpointDAO, ProjectDAO projectDAO) {
		this.endpointDAO = endpointDAO;
		this.projectDAO = projectDAO;
	}

	@Override
	public Map<String, String> getResourceTags(UserInfo userInfo, String endpoint, String project,
											   String customTag) {
		Map<String, String> tags = new HashMap<>();
		tags.put("user_tag", userInfo.getName());
		endpointDAO.get(endpoint)
				.map(EndpointDTO::getTag)
				.ifPresent(tag -> tags.put("endpoint_tag", tag));
			projectDAO.get(project)
				.map(ProjectDTO::getTag)
				.ifPresent(tag -> tags.put("project_tag", tag));
		Optional.ofNullable(customTag).ifPresent(t -> tags.put("custom_tag", t));
		return tags;
	}
}
