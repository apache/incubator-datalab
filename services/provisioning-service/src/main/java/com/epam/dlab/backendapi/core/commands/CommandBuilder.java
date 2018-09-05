/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.backendapi.core.commands;

import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.util.JsonGenerator;
import com.epam.dlab.util.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CommandBuilder {

	public String buildCommand(RunDockerCommand runDockerCommand, ResourceBaseDTO<?> resourceBaseDTO) throws JsonProcessingException {
		StringBuilder builder = new StringBuilder();
		if (resourceBaseDTO != null) {
			builder.append("echo -e '");
			try {
				String str = JsonGenerator.generateJson(resourceBaseDTO);
				log.info("Serialized DTO to: {}", SecurityUtils.hideCreds(str));
				builder.append(str);
			} catch (JsonProcessingException e) {
				log.error("ERROR generating json from dockerRunParameters: {}", e.getMessage());
				throw e;
			}
			builder.append('\'');
			builder.append(" | ");
		}
		builder.append(runDockerCommand.toCMD());
		return builder.toString();
	}
}
