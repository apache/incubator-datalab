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

public class DeployEMRDto extends DeployClusterDto{
    private String emr_instance_count;
    private String emr_master_instance_type;
    private String emr_slave_instance_type;
    private boolean emr_slave_instance_spot = false;
    private Integer emr_slave_instance_spot_pct_price = 0;
    private String emr_version;
    
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
        return super.toString()+MoreObjects.toStringHelper(this)
        		.add("image", getImage())
        		.add("template_name", getTemplate_name())
        		.add("name", getName())
        		.add("notebook_name", getNotebook_name())
        		.add("emr_instance_count", emr_instance_count)
        		.add("emr_master_instance_type", emr_master_instance_type)
        		.add("emr_slave_instance_type", emr_slave_instance_type)
        		.add("emr_slave_instance_spot", emr_slave_instance_spot)
        		.add("emr_slave_instance_spot_pct_price", emr_slave_instance_spot_pct_price)
        		.add("emr_version", emr_version)
        		.toString();
    }
}
