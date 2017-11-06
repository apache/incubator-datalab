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

package com.epam.dlab.automation.model;

import com.google.common.base.MoreObjects;

public class NotebookConfig {

	private String notebook_template;
	private String data_engine_type;
	
	private String timeout_notebook_create="0s";
    private String timeout_notebook_startup="0s";
    private String timeout_notebook_shutdown="0s";
    private String timeout_cluster_create="0s";
    private String timeout_cluster_terminate="0s";
    private String timeout_lib_groups="5m";
    private String timeout_lib_list="5m";
    private String timeout_lib_install="15m";

	public String getTimeout_notebook_create() {
		return timeout_notebook_create;
	}

	public String getTimeout_notebook_startup() {
		return timeout_notebook_startup;
	}

	public String getTimeout_notebook_shutdown() {
		return timeout_notebook_shutdown;
	}

	public String getTimeout_cluster_create() {
		return timeout_cluster_create;
	}

	public String getTimeout_cluster_terminate() {
		return timeout_cluster_terminate;
	}

	public String getTimeout_lib_groups() {
		return timeout_lib_groups;
	}

	public String getTimeout_lib_list() {
		return timeout_lib_list;
	}

	public String getTimeout_lib_install() {
		return timeout_lib_install;
	}

	public String getNotebook_template() {
		return notebook_template;
	}


	public String getData_engine_type() {
		return data_engine_type;
	}


	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("timeout_emr_create", timeout_cluster_create)
				.add("timeout_emr_terminate", timeout_cluster_terminate)
				.add("timeout_lib_groups", timeout_lib_groups)
				.add("timeout_lib_install", timeout_lib_install)
				.add("timeout_lib_list", timeout_lib_list)
				.add("timeout_notebook_create", timeout_notebook_create)
				.add("timeout_notebook_shutdown", timeout_notebook_shutdown)
				.add("timeout_notebook_startup", timeout_notebook_startup)
				.add("notebook_template", notebook_template)
				.add("data_engine_type", data_engine_type)
				.toString();
	}

}
