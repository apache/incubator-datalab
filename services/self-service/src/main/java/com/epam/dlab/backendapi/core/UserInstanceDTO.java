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

package com.epam.dlab.backendapi.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInstanceDTO {
    @JsonProperty("_id")
    private String id;
    @JsonProperty
    private String user;
    @JsonProperty("exploratory_name")
    private String exploratoryName;
    @JsonProperty("exploratory_id")
    private String exploratoryId;
    @JsonProperty("image")
    private String imageName;
    @JsonProperty("version")
    private String imageVersion;
    @JsonProperty
    private String status;
    @JsonProperty
    private String shape;
    @JsonProperty("exploratory_url")
    private String url;
    @JsonProperty("up_time")
    private Date uptime;
    @JsonProperty("computational_resources")
    private List<UserComputationalResourceDTO> resources = new ArrayList<>();

    public String getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public UserInstanceDTO withUser(String user) {
        setUser(user);
        return this;
    }

    public String getExploratoryName() {
        return exploratoryName;
    }

    public void setExploratoryName(String exploratoryName) {
        this.exploratoryName = exploratoryName;
    }

    public UserInstanceDTO withExploratoryName(String exploratoryName) {
        setExploratoryName(exploratoryName);
        return this;
    }

    public String getExploratoryId() {
        return exploratoryId;
    }

    public void setExploratoryId(String exploratoryId) {
        this.exploratoryId = exploratoryId;
    }

    public UserInstanceDTO withExploratoryId(String exploratoryId) {
        setExploratoryId(exploratoryId);
        return this;
    }

    public String getImageName() { return imageName; }

    public void setImageName(String imageName) { this.imageName = imageName; }

    public UserInstanceDTO withImageName(String imageName) {
        setImageName(imageName);
        return this;
    }

    public String getImageVersion() { return imageVersion; }

    public void setImageVersion(String imageVersion) { this.imageVersion = imageVersion; }

    public UserInstanceDTO withImageVersion(String imageVersion) {
        setImageVersion(imageVersion);
        return this;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UserInstanceDTO withStatus(String status) {
        setStatus(status);
        return this;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public UserInstanceDTO withShape(String shape) {
        setShape(shape);
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UserInstanceDTO withUrl(String url) {
        setUrl(url);
        return this;
    }

    public Date getUptime() {
        return uptime;
    }

    public void setUptime(Date uptime) {
        this.uptime = uptime;
    }

    public UserInstanceDTO withUptime(Date uptime) {
        setUptime(uptime);
        return this;
    }

    public List<UserComputationalResourceDTO> getResources() {
        return resources;
    }

    public void setResources(List<UserComputationalResourceDTO> resources) {
        this.resources = resources;
    }

    public UserInstanceDTO withResources(List<UserComputationalResourceDTO> resources) {
        setResources(resources);
        return this;
    }
}
