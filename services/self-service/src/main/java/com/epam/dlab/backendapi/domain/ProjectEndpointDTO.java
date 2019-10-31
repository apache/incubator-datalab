package com.epam.dlab.backendapi.domain;

import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import lombok.Data;

@Data
public class ProjectEndpointDTO {
	private final String name;
	private final UserInstanceStatus status;
	private final EdgeInfo edgeInfo;
}
