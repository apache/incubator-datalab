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

package com.epam.dlab.backendapi.core.commands;

import com.epam.dlab.backendapi.core.response.handlers.ExploratoryCallbackHandler;
import com.epam.dlab.backendapi.core.response.handlers.LibListCallbackHandler;
import com.epam.dlab.backendapi.core.response.handlers.ResourceCallbackHandler;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.rest.client.RESTServiceMock;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Ignore
public class CommandExecutorMockTest {
	private CommandExecutorMock getCommandExecutor() {
		return new CommandExecutorMock(CloudProvider.AWS);
	}

	private CommandExecutorMock executeAsync(String cmd) throws IOException, InterruptedException, ExecutionException {
		String uuid = UUID.randomUUID().toString();
		CommandExecutorMock exec = new CommandExecutorMock(CloudProvider.AWS);
		exec.executeAsync("user", uuid, cmd);
		exec.getResultSync();

		Files.deleteIfExists(Paths.get(exec.getResponseFileName()));

		return exec;
	}

	private String getRequestId(CommandExecutorMock exec) {
		return exec.getVariables().get("request_id");
	}

	private String getEdgeUserName(CommandExecutorMock exec) {
		return exec.getVariables().get("edge_user_name");
	}

	private String getExploratoryName(CommandExecutorMock exec) {
		return exec.getVariables().get("exploratory_name");
	}

	private void handleExploratory(String cmd, DockerAction action) throws Exception {
		String uuid = UUID.randomUUID().toString();
		CommandExecutorMock exec = getCommandExecutor();
		exec.executeAsync("user", uuid, cmd);
		exec.getResultSync();

		RESTServiceMock selfService = new RESTServiceMock();
		ExploratoryCallbackHandler handler = new ExploratoryCallbackHandler(selfService, action,
				getRequestId(exec), getEdgeUserName(exec), getExploratoryName(exec));
		handler.handle(exec.getResponseFileName(), Files.readAllBytes(Paths.get(exec.getResponseFileName())));

		try {
			Files.deleteIfExists(Paths.get(exec.getResponseFileName()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleExploratoryLibs(String cmd, DockerAction action) throws Exception {
		String uuid = UUID.randomUUID().toString();
		CommandExecutorMock exec = getCommandExecutor();
		exec.executeAsync("user", uuid, cmd);
		exec.getResultSync();

		RESTServiceMock selfService = new RESTServiceMock();
		if (action == DockerAction.LIB_INSTALL) {
			throw new Exception("Unimplemented action " + action);
		}
		/*
		ResourceCallbackHandler<?> handler = action.equals(DockerAction.LIB_INSTALL) ?
				new LibInstallCallbackHandler(selfService, action,
				getRequestId(exec), getEdgeUserName(exec), getExploratoryName(exec)):
				new LibListCallbackHandler(selfService, action,
				getRequestId(exec), getEdgeUserName(exec), getExploratoryName(exec));
		*/
		ResourceCallbackHandler<?> handler = new LibListCallbackHandler(selfService, action, getRequestId(exec),
				getEdgeUserName(exec), getExploratoryName(exec));

		handler.handle(exec.getResponseFileName(), Files.readAllBytes(Paths.get(exec.getResponseFileName())));

		try {
			Files.deleteIfExists(Paths.get(exec.getResponseFileName()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void describe() throws IOException, InterruptedException, ExecutionException {
		String cmd =
				"docker run " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/notebook:/logs/notebook " +
						"-e \"conf_resource=notebook\" " +
						"-e \"request_id=28ba67a4-b2ee-4753-a406-892977089ad9\" " +
						"docker.dlab-zeppelin:latest --action describe";
		executeAsync(cmd);
	}

	@Test
	public void edgeCreate() throws IOException, InterruptedException, ExecutionException {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"," +
						"\"conf_service_base_name\":\"usein1120v13\",\"conf_os_family\":\"debian\"," +
						"\"aws_vpc_id\":\"vpc-83c469e4\",\"aws_subnet_id\":\"subnet-22db937a\"," +
						"\"aws_security_groups_ids\":\"sg-4d42dc35\"}' | " +
						"docker run -i --name user_create_edge_1487309918496 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/edge:/logs/edge " +
						"-e \"conf_resource=edge\" " +
						"-e \"request_id=b8267ae6-07b0-44ef-a489-7714b20cf0a4\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-edge --action create";
		executeAsync(cmd);
	}

	@Test
	public void edgeStop() throws IOException, InterruptedException, ExecutionException {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"," +
						"\"conf_service_base_name\":\"usein1122v4\",\"conf_os_family\":\"debian\"}' | " +
						"docker run -i --name user_stop_edge_1487677431773 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/edge:/logs/edge " +
						"-e \"conf_resource=edge\" " +
						"-e \"request_id=2ba3d8f7-654b-48aa-9386-e815b296a957\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-edge --action stop";
		executeAsync(cmd);
	}

	@Test
	public void edgeStart() throws IOException, InterruptedException, ExecutionException {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"," +
						"\"conf_service_base_name\":\"usein1122v4\",\"conf_os_family\":\"debian\"}' | " +
						"docker run -i --name user_start_edge_1487677538220 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/edge:/logs/edge " +
						"-e \"conf_resource=edge\" " +
						"-e \"request_id=d2f6fbae-979e-4b08-9c0d-559a103ec0cc\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-edge --action start";
		executeAsync(cmd);
	}

	@Test
	public void edgeStatus() throws IOException, InterruptedException, ExecutionException {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"," +
						"\"edge_list_resources\":{\"host\":[{\"id\":\"i-05c1a0d0ad030cdc1\"}, " +
						"{\"id\":\"i-05c1a0d0ad030cdc2\"}]}}' | " +
						"docker run -i --name user_status_resources_1487607145484 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/edge:/logs/edge " +
						"-e \"conf_resource=status\" " +
						"-e \"request_id=0fb82e16-deb2-4b18-9ab3-f9f1c12d9e62\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-edge --action status";
		executeAsync(cmd);
	}


	@Test
	public void notebookCreate() throws Exception {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"," +
						"\"conf_service_base_name\":\"usein1120v13\",\"conf_os_family\":\"debian\"," +
						"\"exploratory_name\":\"useinxz1\",\"application\":\"zeppelin\",\"notebook_image\":\"docker" +
						".dlab-zeppelin\"," +
						"\"aws_notebook_instance_type\":\"t2.medium\",\"aws_security_groups_ids\":\"sg-4d42dc35\"}' " +
						"|" +
						" " +
						"docker run -i --name user_create_exploratory_useinxz1_1487312574572 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/notebook:/logs/notebook " +
						"-e \"conf_resource=notebook\" " +
						"-e \"request_id=f720f30b-5949-4919-a50b-ce7af58d6fe9\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-zeppelin --action create";
		handleExploratory(cmd, DockerAction.CREATE);
	}

	@Test
	public void notebookStop() throws Exception {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"," +
						"\"conf_service_base_name\":\"usein1120v13\"," +
						"\"exploratory_name\":\"useinxz1\",\"notebook_image\":\"docker.dlab-zeppelin\"," +
						"\"notebook_instance_name\":\"usein1120v13-user-nb-useinxz1-78af3\"," +
						"\"conf_key_dir\":\"/root/keys\"}' | " +
						"docker run -i --name user_stop_exploratory_useinxz1_1487315364165 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/notebook:/logs/notebook " +
						"-e \"conf_resource=notebook\" " +
						"-e \"request_id=33998e05-7781-432e-b748-bf3f0e7f9342\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-zeppelin --action stop";
		handleExploratory(cmd, DockerAction.STOP);
	}

	@Test
	public void notebookStart() throws Exception {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"," +
						"\"conf_service_base_name\":\"usein1120v13\",\"conf_os_family\":\"debian\"," +
						"\"exploratory_name\":\"useinxz1\",\"notebook_image\":\"docker.dlab-zeppelin\"," +
						"\"notebook_instance_name\":\"usein1120v13-user-nb-useinxz1-78af3\"}' | " +
						"docker run -i --name user_start_exploratory_useinxz1_1487316756857 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/notebook:/logs/notebook " +
						"-e \"conf_resource=notebook\" " +
						"-e \"request_id=d50b9d20-1b1a-415f-8e47-ed0aca029e73\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-zeppelin --action start";
		handleExploratory(cmd, DockerAction.START);
	}

	@Test
	public void notebookTerminate() throws Exception {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"," +
						"\"conf_service_base_name\":\"usein1120v13\",\"conf_os_family\":\"debian\"," +
						"\"exploratory_name\":\"useinxz1\",\"notebook_image\":\"docker.dlab-zeppelin\"," +
						"\"notebook_instance_name\":\"usein1120v13-user-nb-useinxz1-78af3\"}' | " +
						"docker run -i --name user_terminate_exploratory_useinxz1_1487318040180 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/notebook:/logs/notebook " +
						"-e \"conf_resource=notebook\" " +
						"-e \"request_id=de217441-9757-4c4e-b020-548f66b58e00\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-zeppelin --action terminate";
		handleExploratory(cmd, DockerAction.TERMINATE);
	}


	@Test
	public void emrCreate() throws Exception {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"," +
						"\"conf_service_base_name\":\"usein1122v3\",\"conf_os_family\":\"debian\"," +
						"\"exploratory_name\":\"useinj1\",\"application\":\"jupyter\"," +
						"\"computational_name\":\"useine1\"," +
						"\"emr_instance_count\":\"2\",\"emr_master_instance_type\":\"c4.large\"," +
						"\"emr_slave_instance_type\":\"c4.large\"," +
						"\"emr_version\":\"emr-5.2.0\",\"notebook_instance_name\":\"usein1122v3-user-nb-useinj1" +
						"-1b198" +
						"\"," +
						"\"notebook_template_name\":\"Jupyter 1.5\"}' | " +
						"docker run -i --name user_create_computational_useine1_1487653987822 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/emr:/logs/emr " +
						"-e \"conf_resource=emr\" " +
						"-e \"request_id=917db3fd-3c17-4e79-8462-482a71a5d96f\" " +
						"-e \"ec2_role=EMR_EC2_DefaultRole\" " +
						"-e \"emr_timeout=3600\" " +
						"-e \"service_role=EMR_DefaultRole\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-emr --action create";
		executeAsync(cmd);
	}

	@Test
	public void emrConfigure() throws Exception {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"," +
						"\"conf_service_base_name\":\"usein1122v4\",\"exploratory_name\":\"useinj1\"," +
						"\"application\":\"jupyter\",\"computational_name\":\"useine2\",\"emr_version\":\"emr-5.2" +
						".0\"," +
						"\"notebook_instance_name\":\"usein1122v4-user-nb-useinj1-b0a2e\"}' | " +
						"docker run -i --name user_configure_computational_useine2_1487676513703 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/emr:/logs/emr " +
						"-e \"conf_resource=emr\" " +
						"-e \"request_id=dc3c1002-c07d-442b-99f9-18085aeb2881\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-jupyter --action configure";
		executeAsync(cmd);
	}

	@Test
	public void emrTerminate() throws Exception {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"" +
						",\"conf_service_base_name\":\"usein1122v3\",\"exploratory_name\":\"useinj1\"," +
						"\"computational_name\":\"useine1\"," +
						"\"emr_cluster_name\":\"usein1122v3-user-emr-useinj1-useine1-d2db9\"," +
						"\"notebook_instance_name\":\"usein1122v3-user-nb-useinj1-1b198\"," +
						"\"conf_key_dir\":\"/root/keys\"}' | " +
						"docker run -i --name user_terminate_computational_useine1_1487657251858 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/emr:/logs/emr " +
						"-e \"conf_resource=emr\" " +
						"-e \"request_id=2d5c23b8-d312-4fad-8a3c-0b813550d841\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"docker.dlab-emr --action terminate";
		executeAsync(cmd);
	}


	@Test
	public void listLibs() throws Exception {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"" +
						",\"conf_service_base_name\":\"usein1122v3\",\"exploratory_name\":\"useinj1\"," +
						"\"computational_name\":\"useine1\"," +
						"\"emr_cluster_name\":\"usein1122v3-user-emr-useinj1-useine1-d2db9\"," +
						"\"notebook_instance_name\":\"usein1122v3-user-nb-useinj1-1b198\"," +
						"\"conf_key_dir\":\"/root/keys\"}' | " +
						"docker run -i --name user_terminate_computational_useine1_1487657251858 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/emr:/logs/emr " +
						"-e \"conf_resource=notebook\" " +
						"-e \"request_id=2d5c23b8-d312-4fad-8a3c-0b813550d841\" " +
						"-e \"conf_key_name=BDCC-DSS-POC\" " +
						"-e \"application=jupyter\" " +
						"docker.dlab-jupyter --action lib_list";
		executeAsync(cmd);
		handleExploratoryLibs(cmd, DockerAction.LIB_LIST);
	}

	@Test
	public void installLibs() throws Exception {
		String cmd =
				"echo -e '{\"aws_region\":\"us-west-2\",\"aws_iam_user\":\"user@epam.com\"," +
						"\"edge_user_name\":\"user\"" +
						",\"conf_service_base_name\":\"usein1122v3\",\"exploratory_name\":\"useinj1\"," +
						"\"computational_name\":\"useine1\"," +
						"\"emr_cluster_name\":\"usein1122v3-user-emr-useinj1-useine1-d2db9\"," +
						"\"notebook_instance_name\":\"usein1122v3-user-nb-useinj1-1b198\"," +
						"\"conf_key_dir\":\"/root/keys\"}' | " +
						"docker run -i --name user_terminate_computational_useine1_1487657251858 " +
						"-v /home/ubuntu/keys:/root/keys " +
						"-v /opt/dlab/tmp/result:/response " +
						"-v /var/opt/dlab/log/emr:/logs/emr " +
						"-e \"conf_resource=notebook\" " +
						"-e \"additional_libs={'libraries': {\n" +
						"\t\t\t\t'os_pkg': ['nmap', 'htop'],\n" +
						"\t\t\t\t'pip2': ['requests', 'configparser'],\n" +
						"\t\t\t\t'pip3': ['configparser'],\n" +
						"\t\t\t\t'r_pkg': ['rmarkdown']\n" +
						"\t\t\t\t}\n" +
						"\t\t\t\t}\" " +
						"docker.dlab-jupyter --action lib_install";
		executeAsync(cmd);
		handleExploratoryLibs(cmd, DockerAction.LIB_INSTALL);
	}

}
