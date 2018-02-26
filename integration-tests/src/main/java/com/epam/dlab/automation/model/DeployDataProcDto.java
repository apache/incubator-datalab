package com.epam.dlab.automation.model;

import com.google.common.base.MoreObjects;

public class DeployDataProcDto extends DeployClusterDto {
	private String dataproc_master_count;
	private String dataproc_slave_count;
	private String dataproc_preemptible_count;
	private String dataproc_master_instance_type;
	private String dataproc_slave_instance_type;
	private String dataproc_version;

	public String getDataproc_master_count() {
		return dataproc_master_count;
	}

	public void setDataproc_master_count(String dataproc_master_count) {
		this.dataproc_master_count = dataproc_master_count;
	}

	public String getDataproc_slave_count() {
		return dataproc_slave_count;
	}

	public void setDataproc_slave_count(String dataproc_slave_count) {
		this.dataproc_slave_count = dataproc_slave_count;
	}

	public String getDataproc_preemptible_count() {
		return dataproc_preemptible_count;
	}

	public void setDataproc_preemptible_count(String dataproc_preemptible_count) {
		this.dataproc_preemptible_count = dataproc_preemptible_count;
	}

	public String getDataproc_master_instance_type() {
		return dataproc_master_instance_type;
	}

	public void setDataproc_master_instance_type(String dataproc_master_instance_type) {
		this.dataproc_master_instance_type = dataproc_master_instance_type;
	}

	public String getDataproc_slave_instance_type() {
		return dataproc_slave_instance_type;
	}

	public void setDataproc_slave_instance_type(String dataproc_slave_instance_type) {
		this.dataproc_slave_instance_type = dataproc_slave_instance_type;
	}

	public String getDataproc_version() {
		return dataproc_version;
	}

	public void setDataproc_version(String dataproc_version) {
		this.dataproc_version = dataproc_version;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("image", getImage())
				.add("template_name", getTemplate_name())
				.add("name", getName())
				.add("notebook_name", getNotebook_name())
				.add("dataproc_master_count", dataproc_master_count)
				.add("dataproc_slave_count", dataproc_slave_count)
				.add("dataproc_preemptible_count", dataproc_preemptible_count)
				.add("dataproc_master_instance_type", dataproc_master_instance_type)
				.add("dataproc_slave_instance_type", dataproc_slave_instance_type)
				.add("dataproc_version", dataproc_version)
				.toString();
	}
}
