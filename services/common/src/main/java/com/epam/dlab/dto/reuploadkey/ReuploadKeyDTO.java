package com.epam.dlab.dto.reuploadkey;

import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.model.ResourceData;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class ReuploadKeyDTO extends ResourceSysBaseDTO<ReuploadKeyDTO> {

	@JsonProperty
	private String content;

	@JsonProperty
	private List<ResourceData> resources;

	@JsonProperty("resource_id")
	private String resourceId;

	@JsonProperty
	private String id;


	public ReuploadKeyDTO withContent(String content){
		this.content = content;
		return this;
	}

	public ReuploadKeyDTO withResources(List<ResourceData> resources) {
		this.resources = resources;
		return this;
	}

	public ReuploadKeyDTO withId(String id){
		this.id = id;
		return this;
	}

	public ReuploadKeyDTO withResourceId(String resourceId) {
		this.resourceId = resourceId;
		return this;
	}
}
