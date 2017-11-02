package com.epam.dlab.automation.model;

import com.google.common.base.MoreObjects;

public class DeploySparkStandaloneDto {
	
	private String image;
	private String template_name;
	private String name;
	private String notebook_name;
	private String dataengine_instance_count;
	private String dataengine_slave;
	private String dataengine_master;
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
	public String getNotebook_name() {
		return notebook_name;
	}
	public void setNotebook_name(String notebook_name) {
		this.notebook_name = notebook_name;
	}
	public String getDataengine_instance_count() {
		return dataengine_instance_count;
	}
	public void setDataengine_instance_count(String dataengine_instance_count) {
		this.dataengine_instance_count = dataengine_instance_count;
	}
	public String getDataengine_slave() {
		return dataengine_slave;
	}
	public void setDataengine_slave(String dataengine_slave) {
		this.dataengine_slave = dataengine_slave;
	}
	public String getDataengine_master() {
		return dataengine_master;
	}
	public void setDataengine_master(String dataengine_master) {
		this.dataengine_master = dataengine_master;
	}
	
	@Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
        		.add("image", image)
        		.add("template_name", template_name)
        		.add("name", name)
        		.add(dataengine_master, dataengine_master)
        		.add(dataengine_slave, dataengine_slave)
        		.add(dataengine_instance_count, dataengine_instance_count)
        		.add("notebook_name", notebook_name)
        		.toString();
    }
	
	

}
