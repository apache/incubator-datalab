package com.epam.datalab.dto.azure.computational;

import com.epam.datalab.dto.ResourceURL;
import com.epam.datalab.dto.SchedulerJobDTO;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class AzureComputationalResource extends UserComputationalResource {
    @JsonProperty("instance_id")
    private final String instanceId;
    @JsonProperty("master_node_shape")
    private final String masterShape;
    @JsonProperty("slave_node_shape")
    private final String slaveShape;
    @JsonProperty("hdinsight_version")
    private final String version;


    @Builder
    public AzureComputationalResource(String computationalName, String computationalId, String imageName,
                                    String templateName, String status, Date uptime,
                                    SchedulerJobDTO schedulerJobData, boolean reuploadKeyRequired,
                                    String instanceId, String masterShape, String slaveShape, String version,
                                    List<ResourceURL> resourceURL, LocalDateTime lastActivity,
                                    List<ClusterConfig> config, Map<String, String> tags, int totalInstanceCount) {
        super(computationalName, computationalId, imageName, templateName, status, uptime, schedulerJobData,
                reuploadKeyRequired, resourceURL, lastActivity, tags, totalInstanceCount);
        this.instanceId = instanceId;
        this.masterShape = masterShape;
        this.slaveShape = slaveShape;
//        this.slaveSpot = slaveSpot;
//        this.slaveSpotPctPrice = slaveSpotPctPrice;
//        this.slaveNumber = slaveNumber;
        this.version = version;
        this.config = config;
    }
}
