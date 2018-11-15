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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.service.UserSettingService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/user/settings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "User's settings service", authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
public class UserSettingsResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserSettingsResource.class);

	private UserSettingService userSettingService;

	@Inject
	public UserSettingsResource(UserSettingService userSettingService) {
		this.userSettingService = userSettingService;
	}

	@GET
	@ApiOperation("Returns user's settings")
	public String getSettings(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		String settings = userSettingService.getUISettings(userInfo);
		LOGGER.debug("Returns settings for user {}, content is {}", userInfo.getName(), settings);
		return settings;
	}

	@POST
	@ApiOperation("Saves user's settings to database")
	@ApiResponses(@ApiResponse(code = 200, message = "User's settings were saved to database successfully"))
	public Response saveSettings(@ApiParam(hidden = true) @Auth UserInfo userInfo,
								 @ApiParam(value = "Settings data", required = true)
								 @NotBlank String settings) {
		LOGGER.debug("Saves settings for user {}, content is {}", userInfo.getName(), settings);
		userSettingService.saveUISettings(userInfo, settings);
		return Response.ok().build();
	}

	@PUT
	@Path("budget")
	@ApiOperation("Updates allowed budget for users")
	@ApiResponses(@ApiResponse(code = 200, message = "User's settings were updated successfully"))
	@RolesAllowed("/user/settings")
	public Response updateUsersBudget(@ApiParam(hidden = true) @Auth UserInfo userInfo,
									  @Valid @ApiParam @NotEmpty List<UserDTO> budgets) {
		LOGGER.debug("User {} is updating allowed budget for users: {}", userInfo.getName(), budgets);
		userSettingService.updateUsersBudget(budgets);
		return Response.ok().build();
	}


}
