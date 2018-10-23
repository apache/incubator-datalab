package com.epam.dlab.dto.aws.computational;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
public class EmrConfig {
	@JsonProperty("Classification")
	@NotEmpty(message = "'Classification' field should not be empty")
	private String classification;
	@JsonProperty("Properties")
	@NotNull(message = "'Properties' field should not be empty")
	private Map<String, Object> properties;
	@JsonProperty("Configurations")
	private List<EmrConfig> configurations;
}
