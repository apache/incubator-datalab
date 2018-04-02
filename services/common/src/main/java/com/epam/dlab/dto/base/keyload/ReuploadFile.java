package com.epam.dlab.dto.base.keyload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ReuploadFile extends UploadFile {
	@JsonProperty
	private String edgeUserName;
	@JsonProperty
	private Map<String, List<String>> runningEnvironment;
}
