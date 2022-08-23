package com.epam.datalab.dto.azure.computational;

import com.epam.datalab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ComputationalCreateAzure extends ComputationalBase<ComputationalCreateAzure> {
    @JsonProperty("hdinsight_count")
    private String count;
    @JsonProperty("hdinsight_slave_count")
    private String slaveInstanceCount;
    @JsonProperty("hdinsight_master_instance_type")
    private String masterInstanceType;
    @JsonProperty("hdinsight_slave_instance_type")
    private String slaveInstanceType;
    @JsonProperty("hdinsight_version")
    private String version;
    @JsonProperty("conf_shared_image_enabled")
    private String sharedImageEnabled;
}
