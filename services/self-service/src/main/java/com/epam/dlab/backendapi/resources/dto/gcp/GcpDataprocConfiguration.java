package com.epam.dlab.backendapi.resources.dto.gcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Stores limits for creation of the computational resources for Dataproc cluster
 */
@Data
@Builder
public class GcpDataprocConfiguration {
	@NotBlank
	@JsonProperty("min_instance_count")
	private int minInstanceCount;
	@NotBlank
	@JsonProperty("max_instance_count")
	private int maxInstanceCount;
	@NotBlank
	@JsonProperty("min_dataproc_preemptible_instance_count")
	private int minDataprocPreemptibleInstanceCount;
}
