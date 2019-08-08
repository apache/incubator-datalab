/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.backendapi.util;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.resources.dto.BackupFormDTO;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.dto.aws.AwsComputationalCreateForm;
import com.epam.dlab.backendapi.resources.dto.gcp.GcpComputationalCreateForm;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.aws.AwsCloudSettings;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.gcp.GcpCloudSettings;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.ResourceData;
import com.epam.dlab.model.ResourceType;
import com.epam.dlab.model.exploratory.Exploratory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RequestBuilderTest {

	private final String USER = "test";

	private ReuploadKeyDTO expectedReuploadKeyDTO;
	private UserInfo userInfo;
	private Exploratory exploratory;
	private ExploratoryGitCredsDTO egcDto;
	private UserInstanceDTO uiDto;
	private UserComputationalResource computationalResource;

	@Mock
	private SelfServiceApplicationConfiguration configuration;
	@Mock
	private SettingsDAO settingsDAO;

	@InjectMocks
	private RequestBuilder requestBuilder;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		userInfo = getUserInfo();
		expectedReuploadKeyDTO = getReuploadFile();
		exploratory = Exploratory.builder().name("explName").build();
		egcDto = new ExploratoryGitCredsDTO();
		uiDto = new UserInstanceDTO();
		computationalResource = new UserComputationalResource();
	}

	@Test
	public void newEdgeKeyUploadForAWS() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newEdgeKeyUpload(userInfo, "someContent");

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newEdgeKeyUploadForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.isAzureDataLakeEnabled()).thenReturn(true);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		requestBuilder.newEdgeKeyUpload(userInfo, "someContent");

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).isAzureDataLakeEnabled();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newEdgeKeyUploadForGCP() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newEdgeKeyUpload(userInfo, "someContent");

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newEdgeKeyUploadWithException() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		doThrow(new RuntimeException()).when(settingsDAO).getAwsRegion();

		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Cannot create instance of resource class ");

		requestBuilder.newEdgeKeyUpload(userInfo, "someContent");
	}

	@Test
	public void newKeyReuploadForAwsOrAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);

		ReuploadKeyDTO actualReuploadFile = requestBuilder.newKeyReupload(userInfo, "someId", "someContent",
				Collections.singletonList(
						new ResourceData(ResourceType.EXPLORATORY, "someId", "someName", null)));
		AwsCloudSettings cloudSettings = new AwsCloudSettings();
		cloudSettings.setAwsIamUser(USER);
		expectedReuploadKeyDTO.withCloudSettings(cloudSettings);
		expectedReuploadKeyDTO.withId("someId");
		expectedReuploadKeyDTO.withResources(Collections.singletonList(
				new ResourceData(ResourceType.EXPLORATORY, "someId", "someName", null)));
		assertEquals(expectedReuploadKeyDTO.getId(), actualReuploadFile.getId());
		assertEquals(expectedReuploadKeyDTO.getContent(), actualReuploadFile.getContent());
		assertEquals(expectedReuploadKeyDTO.getResources(), actualReuploadFile.getResources());
		assertEquals(expectedReuploadKeyDTO.getCloudSettings(), actualReuploadFile.getCloudSettings());
		assertEquals(expectedReuploadKeyDTO.getConfKeyDir(), actualReuploadFile.getConfKeyDir());
		assertEquals(expectedReuploadKeyDTO.getConfOsFamily(), actualReuploadFile.getConfOsFamily());
		assertEquals(expectedReuploadKeyDTO.getEdgeUserName(), actualReuploadFile.getEdgeUserName());
		assertEquals(expectedReuploadKeyDTO.getServiceBaseName(), actualReuploadFile.getServiceBaseName());

		verify(configuration, times(2)).getCloudProvider();
		verifyNoMoreInteractions(configuration);
	}

	@Test
	public void newKeyReuploadForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);

		ReuploadKeyDTO actualReuploadFile = requestBuilder.newKeyReupload(userInfo, "someId", "someContent",
				Collections.singletonList(
						new ResourceData(ResourceType.EXPLORATORY, "someId", "someName", null)));
		GcpCloudSettings cloudSettings = new GcpCloudSettings();
		cloudSettings.setGcpIamUser(USER);
		expectedReuploadKeyDTO.withCloudSettings(cloudSettings);
		expectedReuploadKeyDTO.withId("someId");
		expectedReuploadKeyDTO.withResources(Collections.singletonList(
				new ResourceData(ResourceType.EXPLORATORY, "someId", "someName", null)));
		assertEquals(expectedReuploadKeyDTO.getId(), actualReuploadFile.getId());
		assertEquals(expectedReuploadKeyDTO.getContent(), actualReuploadFile.getContent());
		assertEquals(expectedReuploadKeyDTO.getResources(), actualReuploadFile.getResources());
		assertEquals(expectedReuploadKeyDTO.getCloudSettings(), actualReuploadFile.getCloudSettings());
		assertEquals(expectedReuploadKeyDTO.getConfKeyDir(), actualReuploadFile.getConfKeyDir());
		assertEquals(expectedReuploadKeyDTO.getConfOsFamily(), actualReuploadFile.getConfOsFamily());
		assertEquals(expectedReuploadKeyDTO.getEdgeUserName(), actualReuploadFile.getEdgeUserName());
		assertEquals(expectedReuploadKeyDTO.getServiceBaseName(), actualReuploadFile.getServiceBaseName());

		verify(configuration, times(2)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verifyNoMoreInteractions(configuration);
	}

	@Test
	public void newEdgeAction() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newEdgeAction(userInfo);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newEdgeActionWithException() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		doThrow(new RuntimeException()).when(settingsDAO).getAwsRegion();

		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Cannot create instance of resource class ");

		requestBuilder.newEdgeAction(userInfo);
	}

	@Test
	public void newUserEnvironmentStatus() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newUserEnvironmentStatus(userInfo);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newUserEnvironmentStatusWithException() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		doThrow(new RuntimeException()).when(settingsDAO).getAwsRegion();

		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Cannot create instance of resource class ");

		requestBuilder.newUserEnvironmentStatus(userInfo);
	}

	@Test
	public void newExploratoryCreateForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newExploratoryCreate(exploratory, userInfo, egcDto, null);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryCreateForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.isAzureDataLakeEnabled()).thenReturn(true);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");
		when(settingsDAO.getAzureDataLakeClientId()).thenReturn("someId");

		requestBuilder.newExploratoryCreate(exploratory, userInfo, egcDto, null);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO, times(2)).isAzureDataLakeEnabled();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryCreateForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newExploratoryCreate(exploratory, userInfo, egcDto, null);

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryStartForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newExploratoryStart(userInfo, uiDto, egcDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryStartForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.isAzureDataLakeEnabled()).thenReturn(true);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");
		when(settingsDAO.getAzureDataLakeClientId()).thenReturn("someId");

		requestBuilder.newExploratoryStart(userInfo, uiDto, egcDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO, times(2)).isAzureDataLakeEnabled();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryStartForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newExploratoryStart(userInfo, uiDto, egcDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryStopForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newExploratoryStop(userInfo, uiDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryStopForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		requestBuilder.newExploratoryStop(userInfo, uiDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryStopForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newExploratoryStop(userInfo, uiDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newGitCredentialsUpdateForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newGitCredentialsUpdate(userInfo, uiDto, egcDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newGitCredentialsUpdateForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		requestBuilder.newGitCredentialsUpdate(userInfo, uiDto, egcDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newGitCredentialsUpdateForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newGitCredentialsUpdate(userInfo, uiDto, egcDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibInstallForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newLibInstall(userInfo, uiDto, new ArrayList<LibInstallDTO>());

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibInstallForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		requestBuilder.newLibInstall(userInfo, uiDto, new ArrayList<LibInstallDTO>());

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibInstallForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newLibInstall(userInfo, uiDto, new ArrayList<LibInstallDTO>());

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibExploratoryListForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newLibExploratoryList(userInfo, uiDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibExploratoryListForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		requestBuilder.newLibExploratoryList(userInfo, uiDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibExploratoryListForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newLibExploratoryList(userInfo, uiDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibInstallWithComputationalResourceForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newLibInstall(userInfo, uiDto, computationalResource, new ArrayList<LibInstallDTO>());

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibInstallWithComputationalResourceForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		requestBuilder.newLibInstall(userInfo, uiDto, computationalResource, new ArrayList<LibInstallDTO>());

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibInstallWithComputationalResourceForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newLibInstall(userInfo, uiDto, computationalResource, new ArrayList<LibInstallDTO>());

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibComputationalListForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newLibComputationalList(userInfo, uiDto, computationalResource);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibComputationalListForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		requestBuilder.newLibComputationalList(userInfo, uiDto, computationalResource);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newLibComputationalListForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newLibComputationalList(userInfo, uiDto, computationalResource);

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalCreateForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		ComputationalCreateFormDTO form = new AwsComputationalCreateForm();

		requestBuilder.newComputationalCreate(userInfo, uiDto, form);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalCreateForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);

		expectedException.expect(UnsupportedOperationException.class);
		requestBuilder.newComputationalCreate(userInfo, uiDto, new AwsComputationalCreateForm());
	}

	@Test
	public void newComputationalCreateForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		ComputationalCreateFormDTO form = new GcpComputationalCreateForm();

		requestBuilder.newComputationalCreate(userInfo, uiDto, form);

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalCreateWithSparkForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		SparkStandaloneClusterCreateForm form = new SparkStandaloneClusterCreateForm();

		requestBuilder.newComputationalCreate(userInfo, uiDto, form);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalCreateWithSparkForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.isAzureDataLakeEnabled()).thenReturn(true);
		when(settingsDAO.getAzureDataLakeClientId()).thenReturn("someDlId");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		SparkStandaloneClusterCreateForm form = new SparkStandaloneClusterCreateForm();

		requestBuilder.newComputationalCreate(userInfo, uiDto, form);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO, times(2)).isAzureDataLakeEnabled();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
	}

	@Test
	public void newComputationalCreateWithSparkForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		SparkStandaloneClusterCreateForm form = new SparkStandaloneClusterCreateForm();

		requestBuilder.newComputationalCreate(userInfo, uiDto, form);

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}


	@Test
	public void newComputationalTerminateForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newComputationalTerminate(userInfo, "explName", "explId", "compName",
				"compId", DataEngineType.CLOUD_SERVICE, "");

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalTerminateForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		requestBuilder.newComputationalTerminate(userInfo, "explName", "explId", "compName",
				"compId", DataEngineType.CLOUD_SERVICE, "");

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalTerminateForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newComputationalTerminate(userInfo, "explName", "explId", "compName",
				"compId", DataEngineType.CLOUD_SERVICE, "");

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}


	@Test
	public void newComputationalStopForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		final UserInstanceDTO exploratory = new UserInstanceDTO();
		exploratory.setExploratoryId("explId");
		exploratory.setExploratoryName("explName");
		requestBuilder.newComputationalStop(userInfo, exploratory, "compName");

		verify(configuration, times(2)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalStopForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		final UserInstanceDTO exploratory = new UserInstanceDTO();
		exploratory.setExploratoryId("explId");
		exploratory.setExploratoryName("explName");
		requestBuilder.newComputationalStop(userInfo, exploratory, "compName");

		verify(configuration, times(2)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalStopForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		final UserInstanceDTO exploratory = new UserInstanceDTO();
		exploratory.setExploratoryId("explId");
		exploratory.setExploratoryName("explName");
		requestBuilder.newComputationalStop(userInfo, exploratory, "compName");

		verify(configuration, times(2)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalStartForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		final UserInstanceDTO exploratory = new UserInstanceDTO();
		exploratory.setExploratoryId("explId");
		exploratory.setExploratoryName("explName");

		requestBuilder.newComputationalStart(userInfo, exploratory, "compName");

		verify(configuration, times(2)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalStartForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		final UserInstanceDTO exploratory = new UserInstanceDTO();
		exploratory.setExploratoryId("explId");
		exploratory.setExploratoryName("explName");
		requestBuilder.newComputationalStart(userInfo, exploratory, "compName");

		verify(configuration, times(2)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalStartForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		final UserInstanceDTO exploratory = new UserInstanceDTO();
		exploratory.setExploratoryId("explId");
		exploratory.setExploratoryName("explName");
		requestBuilder.newComputationalStart(userInfo, exploratory, "compName");

		verify(configuration, times(2)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryImageCreateForAws() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AWS);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAwsRegion()).thenReturn("someAwsRegion");
		when(settingsDAO.getAwsSecurityGroups()).thenReturn("someAwsSecurityGroups");
		when(settingsDAO.getAwsSubnetId()).thenReturn("someAwsSubnetId");
		when(settingsDAO.getAwsVpcId()).thenReturn("someAwsVpcId");
		when(settingsDAO.getConfTagResourceId()).thenReturn("someConfTagResourceId");

		requestBuilder.newExploratoryImageCreate(userInfo, uiDto, "compName");

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
		verify(settingsDAO).getAwsNotebookSubnetId();
		verify(settingsDAO).getAwsNotebookVpcId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryImageCreateForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		requestBuilder.newExploratoryImageCreate(userInfo, uiDto, "compName");

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryImageCreateForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newExploratoryImageCreate(userInfo, uiDto, "compName");

		verify(configuration, times(3)).getCloudProvider();
		verify(configuration).getMaxUserNameLength();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}


	@Test
	public void newBackupCreate() {
		List<String> configFiles = new ArrayList<>();
		List<String> keys = new ArrayList<>();
		List<String> certificates = new ArrayList<>();
		List<String> jars = new ArrayList<>();
		BackupFormDTO backupFormDTO = new BackupFormDTO(configFiles, keys, certificates,
				jars, true, true);
		EnvBackupDTO expectedEnvBackupDTO = EnvBackupDTO.builder().configFiles(configFiles).keys(keys)
				.certificates(certificates).jars(jars).databaseBackup(true).logsBackup(true).id("someId").build();
		EnvBackupDTO actualEnvBackupDTO = requestBuilder.newBackupCreate(backupFormDTO, "someId");
		assertEquals(expectedEnvBackupDTO, actualEnvBackupDTO);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, "token");
	}

	private ReuploadKeyDTO getReuploadFile() {
		return new ReuploadKeyDTO().withContent("someContent").withEdgeUserName(USER).withContent("someContent");
	}
}