package com.epam.dlab.automation.model;

import com.google.common.base.MoreObjects;

public class DeployDataProcDto extends DeployClusterDto {
	private String dataproc_instance_count;
	private String dataproc_master_instance_type;
	private String dataproc_slave_instance_type;
	private boolean dataproc_slave_instance_spot = false;
	private Integer dataproc_slave_instance_spot_pct_price = 0;
	private String dataproc_version;

	public String getDataproc_instance_count() {
		return dataproc_instance_count;
	}

	public void setDataproc_instance_count(String dataproc_instance_count) {
		this.dataproc_instance_count = dataproc_instance_count;
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


	public boolean isDataproc_slave_instance_spot() {
		return dataproc_slave_instance_spot;
	}

	public void setDataproc_slave_instance_spot(boolean dataproc_slave_instance_spot) {
		this.dataproc_slave_instance_spot = dataproc_slave_instance_spot;
	}

	public Integer getDataproc_slave_instance_spot_pct_price() {
		return dataproc_slave_instance_spot_pct_price;
	}

	public void setDataproc_slave_instance_spot_pct_price(Integer dataproc_slave_instance_spot_pct_price) {
		this.dataproc_slave_instance_spot_pct_price = dataproc_slave_instance_spot_pct_price;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("image", getImage())
				.add("template_name", getTemplate_name())
				.add("name", getName())
				.add("notebook_name", getNotebook_name())
				.add("dataproc_instance_count", dataproc_instance_count)
				.add("dataproc_master_instance_type", dataproc_master_instance_type)
				.add("dataproc_slave_instance_type", dataproc_slave_instance_type)
				.add("dataproc_slave_instance_spot", dataproc_slave_instance_spot)
				.add("dataproc_slave_instance_spot_pct_price", dataproc_slave_instance_spot_pct_price)
				.add("dataproc_version", dataproc_version)
				.toString();
	}
}
