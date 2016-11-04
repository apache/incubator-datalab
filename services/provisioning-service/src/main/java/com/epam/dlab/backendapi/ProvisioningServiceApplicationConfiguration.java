/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

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
