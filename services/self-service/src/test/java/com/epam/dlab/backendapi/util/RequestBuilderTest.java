package com.epam.dlab.backendapi.util;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.resources.dto.BackupFormDTO;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.dto.aws.AwsComputationalCreateForm;
import com.epam.dlab.backendapi.resources.dto.gcp.GcpComputationalCreateForm;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.keyload.ReuploadFile;
import com.epam.dlab.dto.base.keyload.UploadFile;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.exloratory.Exploratory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RequestBuilderTest {

	private final String USER = "test";

	private ReuploadFile expectedReuploadFile;
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
		expectedReuploadFile = getReuploadFile();
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

		UploadFile actualReuploadFile = requestBuilder.newKeyReupload(userInfo, "someContent");
		assertEquals(expectedReuploadFile, actualReuploadFile);

		verify(configuration).getCloudProvider();
		verifyNoMoreInteractions(configuration);
	}

	@Test
	public void newKeyReuploadForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);

		UploadFile actualReuploadFile = requestBuilder.newKeyReupload(userInfo, "someContent");
		assertEquals(expectedReuploadFile, actualReuploadFile);

		verify(configuration).getCloudProvider();
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

		requestBuilder.newExploratoryCreate(exploratory, userInfo, egcDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
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

		requestBuilder.newExploratoryCreate(exploratory, userInfo, egcDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO, times(2)).isAzureDataLakeEnabled();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verify(settingsDAO).getAzureDataLakeClientId();
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newExploratoryCreateForGcp() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.GCP);
		when(configuration.getMaxUserNameLength()).thenReturn(10);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		requestBuilder.newExploratoryCreate(exploratory, userInfo, egcDto);

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
		verify(settingsDAO).getAzureDataLakeClientId();
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

		requestBuilder.newLibInstall(userInfo, uiDto);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
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

		requestBuilder.newLibInstall(userInfo, uiDto);

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

		requestBuilder.newLibInstall(userInfo, uiDto);

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

		requestBuilder.newLibInstall(userInfo, uiDto, computationalResource);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
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

		requestBuilder.newLibInstall(userInfo, uiDto, computationalResource);

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

		requestBuilder.newLibInstall(userInfo, uiDto, computationalResource);

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
		verifyNoMoreInteractions(configuration, settingsDAO);
	}

	@Test
	public void newComputationalCreateForAzure() {
		when(configuration.getCloudProvider()).thenReturn(CloudProvider.AZURE);
		when(settingsDAO.getServiceBaseName()).thenReturn("someSBN");
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");
		when(settingsDAO.getAzureRegion()).thenReturn("someAzureRegion");
		when(settingsDAO.getAzureResourceGroupName()).thenReturn("someAzureResourceGroup");
		when(settingsDAO.getAzureSecurityGroupName()).thenReturn("someAzureResourceGroupName");
		when(settingsDAO.getAzureSubnetName()).thenReturn("someAzureSubnetId");
		when(settingsDAO.getAzureVpcName()).thenReturn("someAzureVpcId");

		ComputationalCreateFormDTO form = new AwsComputationalCreateForm();

		requestBuilder.newComputationalCreate(userInfo, uiDto, form);

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
		verify(settingsDAO).getAzureDataLakeClientId();
		verify(settingsDAO).getAzureRegion();
		verify(settingsDAO).getAzureResourceGroupName();
		verify(settingsDAO).getAzureSecurityGroupName();
		verify(settingsDAO).getAzureSubnetName();
		verify(settingsDAO).getAzureVpcName();
		verifyNoMoreInteractions(configuration, settingsDAO);
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
				"compId", DataEngineType.CLOUD_SERVICE);

		verify(configuration, times(3)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
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
				"compId", DataEngineType.CLOUD_SERVICE);

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
				"compId", DataEngineType.CLOUD_SERVICE);

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

		requestBuilder.newComputationalStop(userInfo, "explName", "explId", "compName");

		verify(configuration, times(2)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
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

		requestBuilder.newComputationalStop(userInfo, "explName", "explId", "compName");

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

		requestBuilder.newComputationalStop(userInfo, "explName", "explId", "compName");

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

		requestBuilder.newComputationalStart(userInfo, "explName", "explId", "compName");

		verify(configuration, times(2)).getCloudProvider();
		verify(settingsDAO).getServiceBaseName();
		verify(settingsDAO).getConfOsFamily();
		verify(settingsDAO).getAwsRegion();
		verify(settingsDAO).getAwsSecurityGroups();
		verify(settingsDAO).getAwsSubnetId();
		verify(settingsDAO).getAwsVpcId();
		verify(settingsDAO).getConfTagResourceId();
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

		requestBuilder.newComputationalStart(userInfo, "explName", "explId", "compName");

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

		requestBuilder.newComputationalStart(userInfo, "explName", "explId", "compName");

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

	private ReuploadFile getReuploadFile() {
		ReuploadFile reuploadFile = new ReuploadFile();
		reuploadFile.setContent("someContent");
		reuploadFile.setEdgeUserName(USER);
		return reuploadFile;
	}
}