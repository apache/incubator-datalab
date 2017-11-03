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

	public String getNotebook_template() {
		return notebook_template;
	}

	public void setNotebook_template(String notebook_template) {
		this.notebook_template = notebook_template;
	}

	public String getData_engine_type() {
		return data_engine_type;
	}

	public void setData_engine_type(String data_engine_type) {
		this.data_engine_type = data_engine_type;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("notebook_template", notebook_template)
				.add("data_engine_type", data_engine_type)
				.toString();
	}

}
