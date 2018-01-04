package com.epam.dlab.dto.gcp;

import com.epam.dlab.dto.base.CloudSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class GcpCloudSettings extends CloudSettings {

    @JsonProperty("gcp_iam_user")
    private String gcpIamUser;

    @JsonProperty("gcp_subnet_name")
    private String gcpSubnetName;

    @JsonProperty("gcp_vpc_name")
    private String gcpVpcName;

    @JsonProperty("gcp_region")
    private String gcpRegion;

    @JsonProperty("gcp_project_id")
    private String gcpProjectId;

    @Builder
    public GcpCloudSettings(String gcpIamUser, String gcpSubnetName, String gcpVpcName, String gcpRegion, String gcpProjectId) {
        this.gcpIamUser = gcpIamUser;
        this.gcpSubnetName = gcpSubnetName;
        this.gcpVpcName = gcpVpcName;
        this.gcpRegion = gcpRegion;
        this.gcpProjectId = gcpProjectId;
    }

    @Override
    @JsonIgnore
    public String getIamUser() {
        return gcpIamUser;
    }
}