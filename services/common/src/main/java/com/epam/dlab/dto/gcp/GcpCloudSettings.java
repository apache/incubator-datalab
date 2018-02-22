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

    @Builder
    public GcpCloudSettings(String gcpIamUser) {
        this.gcpIamUser = gcpIamUser;
    }

    @Override
    @JsonIgnore
    public String getIamUser() {
        return gcpIamUser;
    }
}