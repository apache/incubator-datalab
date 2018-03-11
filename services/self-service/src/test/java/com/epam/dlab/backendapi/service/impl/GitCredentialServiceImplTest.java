package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.GitCredsDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.epam.dlab.rest.client.RESTService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GitCredentialServiceImplTest {

	private final String USER = "test";

	@Mock
	private GitCredsDAO gitCredsDAO;
	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private RESTService provisioningService;
	@Mock
	private RequestBuilder requestBuilder;
	@Mock
	private RequestId requestId;

	@InjectMocks
	private GitCredentialServiceImpl gitCredentialService;

	@Test
	public void updateGitCredentials() {
		String token = "token";
		UserInfo userInfo = new UserInfo(USER, token);
		doNothing().when(gitCredsDAO).updateGitCreds(anyString(), any(ExploratoryGitCredsDTO.class));

		String exploratoryName = "explName";
		UserInstanceDTO uiDto = new UserInstanceDTO().withExploratoryName(exploratoryName).withUser(USER);
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(Collections.singletonList(uiDto));

		ExploratoryGitCredsUpdateDTO egcuDto = new ExploratoryGitCredsUpdateDTO().withExploratoryName(exploratoryName);
		when(requestBuilder.newGitCredentialsUpdate(any(UserInfo.class), any(UserInstanceDTO.class),
				any(ExploratoryGitCredsDTO.class))).thenReturn(egcuDto);

		String uuid = "someUuid";
		when(provisioningService.post(anyString(), anyString(), any(ExploratoryGitCredsUpdateDTO.class), any()))
				.thenReturn(uuid);
		when(requestId.put(anyString(), anyString())).thenReturn(uuid);

		ExploratoryGitCredsDTO egcDto = new ExploratoryGitCredsDTO();
		gitCredentialService.updateGitCredentials(userInfo, egcDto);

		verify(gitCredsDAO).updateGitCreds(USER, egcDto);
		verifyNoMoreInteractions(gitCredsDAO);

		verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
		verifyNoMoreInteractions(exploratoryDAO);

		verify(requestBuilder).newGitCredentialsUpdate(userInfo, uiDto, egcDto);
		verifyNoMoreInteractions(requestBuilder);

		verify(provisioningService).post("exploratory/git_creds", token, egcuDto, String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, uuid);
		verifyNoMoreInteractions(requestId);
	}

	@Test
	public void getGitCredentials() {
		ExploratoryGitCredsDTO expectedEgcDto = new ExploratoryGitCredsDTO();
		when(gitCredsDAO.findGitCreds(anyString(), anyBoolean())).thenReturn(expectedEgcDto);

		ExploratoryGitCredsDTO actualEgcDto = gitCredentialService.getGitCredentials(USER);
		assertNotNull(actualEgcDto);
		assertEquals(expectedEgcDto, actualEgcDto);

		verify(gitCredsDAO).findGitCreds(USER, true);
		verifyNoMoreInteractions(gitCredsDAO);
	}
}
