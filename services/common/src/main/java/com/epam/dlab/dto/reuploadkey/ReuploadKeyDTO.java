package com.epam.dlab.dto.reuploadkey;

import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class ReuploadKeyDTO extends ResourceSysBaseDTO<ReuploadKeyDTO> {

	@JsonProperty
	private String content;

	@JsonProperty("list_instances_names")
	private List<String> runningResources;

	private String id;

	public ReuploadKeyDTO withContent(String content){
		this.content = content;
		return this;
	}

	public ReuploadKeyDTO withRunningResources(List<String> runningResources){
		this.runningResources = runningResources;
		return this;
	}

	public ReuploadKeyDTO withId(String id){
		this.id = id;
		return this;
	}

}
