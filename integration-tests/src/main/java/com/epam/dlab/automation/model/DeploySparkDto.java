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

public class DeploySparkDto extends DeployClusterDto{
	
	private String dataengine_instance_count;
	private String dataengine_instance_shape;
	
	public String getDataengine_instance_count() {
		return dataengine_instance_count;
	}
	public void setDataengine_instance_count(String dataengine_instance_count) {
		this.dataengine_instance_count = dataengine_instance_count;
	}

	public String getDataengine_instance_shape() {
		return dataengine_instance_shape;
	}

	public void setDataengine_instance_shape(String dataengine_instance_shape) {
		this.dataengine_instance_shape = dataengine_instance_shape;
	}

	@Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
        		.add("image", getImage())
        		.add("template_name", getTemplate_name())
        		.add("name", getName())
        		.add("notebook_name", getNotebook_name())
				.add("dataengine_instance_shape", dataengine_instance_shape)
        		.add("dataengine_instance_count", dataengine_instance_count)
        		.toString();
    }
	
	

}
