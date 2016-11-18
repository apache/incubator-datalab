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

package com.epam.dlab.backendapi;

import com.epam.dlab.backendapi.core.response.Directories;
import com.epam.dlab.client.restclient.RESTServiceFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ProvisioningServiceApplicationConfiguration extends Configuration implements Directories {
    public static final String SELF_SERVICE = "selfService";

    @NotEmpty
    @JsonProperty
    private String keyDirectory;

    @NotEmpty
    @JsonProperty
    private String responseDirectory;

    @JsonProperty
    private Duration warmupPollTimeout = Duration.seconds(3);

    @JsonProperty
    private Duration resourceStatusPollTimeout = Duration.minutes(3);

    @JsonProperty
    private Duration keyLoaderPollTimeout = Duration.minutes(2);

    @NotEmpty
    @JsonProperty
    private String adminKey;

    @NotEmpty
    @JsonProperty
    private String edgeImage;

    @JsonProperty
    private Duration fileLengthCheckDelay = Duration.seconds(3);

    @NotEmpty
    @JsonProperty
    private String notebookImage;

    @NotEmpty
    @JsonProperty
    private String emrImage;

    @NotEmpty
    @JsonProperty
    private String emrEC2RoleDefault;

    @NotEmpty
    @JsonProperty
    private String emrServiceRoleDefault;

    @Valid
    @NotNull
    @JsonProperty(SELF_SERVICE)
    private RESTServiceFactory selfFactory = new RESTServiceFactory();

    public String getKeyDirectory() {
        return keyDirectory;
    }

    public Duration getWarmupPollTimeout() {
        return warmupPollTimeout;
    }

    public Duration getResourceStatusPollTimeout() {
        return resourceStatusPollTimeout;
    }

    public Duration getKeyLoaderPollTimeout() {
        return keyLoaderPollTimeout;
    }

    public String getAdminKey() {
        return adminKey;
    }

    public String getEdgeImage() {
        return edgeImage;
    }

    public Duration getFileLengthCheckDelay() {
        return fileLengthCheckDelay;
    }

    public String getNotebookImage() {
        return notebookImage;
    }

    public String getEmrImage() {
        return emrImage;
    }

    public String getEmrEC2RoleDefault() {
        return emrEC2RoleDefault;
    }

    public String getEmrServiceRoleDefault() {
        return emrServiceRoleDefault;
    }

    public RESTServiceFactory getSelfFactory() {
        return selfFactory;
    }

    public String getWarmupDirectory() {
        return responseDirectory + WARMUP_DIRECTORY;
    }

    public String getImagesDirectory() {
        return responseDirectory + IMAGES_DIRECTORY;
    }

    public String getKeyLoaderDirectory() {
        return responseDirectory + KEY_LOADER_DIRECTORY;
    }
}
