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

public class DeployEMRDto {
    private String image;
    private String template_name;
    private String name;
    private String emr_instance_count;
    private String emr_master_instance_type;
    private String emr_slave_instance_type;
    private boolean emr_slave_instance_spot = false;
    private Integer emr_slave_instance_spot_pct_price = 0;
    private String emr_version;
    private String notebook_name;
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public String getTemplate_name() {
        return template_name;
    }
    
    public void setTemplate_name(String template_name) {
        this.template_name = template_name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmr_instance_count() {
        return emr_instance_count;
    }
    
    public void setEmr_instance_count(String emr_instance_count) {
        this.emr_instance_count = emr_instance_count;
    }
    
    public String getEmr_master_instance_type() {
        return emr_master_instance_type;
    }
    
    public void setEmr_master_instance_type(String emr_master_instance_type) {
        this.emr_master_instance_type = emr_master_instance_type;
    }
    
    public String getEmr_slave_instance_type() {
        return emr_slave_instance_type;
    }
    
    public void setEmr_slave_instance_type(String emr_slave_instance_type) {
        this.emr_slave_instance_type = emr_slave_instance_type;
    }
    
    public String getEmr_version() {
        return emr_version;
    }
    
    public void setEmr_version(String emr_version) {
        this.emr_version = emr_version;
    }
    
    public String getNotebook_name() {
        return notebook_name;
    }
    
    public void setNotebook_name(String notebook_name) {
        this.notebook_name = notebook_name;
    }

    public boolean isEmr_slave_instance_spot() {
        return emr_slave_instance_spot;
    }

    public void setEmr_slave_instance_spot(boolean emr_slave_instance_spot) {
        this.emr_slave_instance_spot = emr_slave_instance_spot;
    }

    public Integer getEmr_slave_instance_spot_pct_price() {
        return emr_slave_instance_spot_pct_price;
    }

    public void setEmr_slave_instance_spot_pct_price(Integer emr_slave_instance_spot_pct_price) {
        this.emr_slave_instance_spot_pct_price = emr_slave_instance_spot_pct_price;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
        		.add("image", image)
        		.add("template_name", template_name)
        		.add("name", name)
        		.add("emr_instance_count", emr_instance_count)
        		.add("emr_master_instance_type", emr_master_instance_type)
        		.add("emr_slave_instance_type", emr_slave_instance_type)
        		.add("emr_slave_instance_spot", emr_slave_instance_spot)
        		.add("emr_slave_instance_spot_pct_price", emr_slave_instance_spot_pct_price)
        		.add("emr_version", emr_version)
        		.add("notebook_name", notebook_name)
        		.toString();
    }
}
