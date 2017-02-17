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

import com.epam.dlab.dto.exploratory.ExploratoryURL;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Stores info about the user notebook.
 */
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
    @JsonProperty("template_name")
    private String templateName;
    @JsonProperty
    private String status;
    @JsonProperty
    private String shape;
    @JsonProperty("exploratory_url")
    private List<ExploratoryURL> exploratoryUrl;
    @JsonProperty("up_time")
    private Date uptime;
    @JsonProperty("computational_resources")
    private List<UserComputationalResourceDTO> resources = new ArrayList<>();
    @JsonProperty("private_ip")
    private String privateIp;


    /** Returns the unique id for the notebook. */
    public String getId() {
        return id;
    }

    /** Returns the user login name. */
    public String getUser() {
        return user;
    }

    /** Sets the user login name. */
    public void setUser(String user) {
        this.user = user;
    }

    /** Sets the user login name. */
    public UserInstanceDTO withUser(String user) {
        setUser(user);
        return this;
    }

    /** Returns the name of exploratory. */
    public String getExploratoryName() {
        return exploratoryName;
    }

    /** Sets the name of exploratory. */
    public void setExploratoryName(String exploratoryName) {
        this.exploratoryName = exploratoryName;
    }

    /** Sets the name of exploratory. */
    public UserInstanceDTO withExploratoryName(String exploratoryName) {
        setExploratoryName(exploratoryName);
        return this;
    }

    /** Returns the exploratory id. */
    public String getExploratoryId() {
        return exploratoryId;
    }

    /** Sets the exploratory id. */
    public void setExploratoryId(String exploratoryId) {
        this.exploratoryId = exploratoryId;
    }

    /** Sets the exploratory id. */
    public UserInstanceDTO withExploratoryId(String exploratoryId) {
        setExploratoryId(exploratoryId);
        return this;
    }

    /** Returns the image name. */
    public String getImageName() {
    	return imageName;
    }

    /** Sets the image name. */
    public void setImageName(String imageName) {
    	this.imageName = imageName;
    }

    /** Sets the image name. */
    public UserInstanceDTO withImageName(String imageName) {
        setImageName(imageName);
        return this;
    }

    /** Returns the image version. */
    public String getImageVersion() {
    	return imageVersion;
    }

    /** Sets the image version. */
    public void setImageVersion(String imageVersion) {
    	this.imageVersion = imageVersion;
    }

    /** Sets the image version. */
    public UserInstanceDTO withImageVersion(String imageVersion) {
        setImageVersion(imageVersion);
        return this;
    }

    /** Returns the name of template. */
    public String getTemplateName() {
    	return templateName;
    }

    /** Sets the name of template. */
    public void setTemplateName(String templateName) {
    	this.templateName = templateName;
    }

    /** Sets the name of template. */
    public UserInstanceDTO withTemplateName(String templateName) {
        setTemplateName(templateName);
        return this;
    }

    /** Returns the status of notebook. */
    public String getStatus() {
        return status;
    }

    /** Sets the status of notebook. */
    public void setStatus(String status) {
        this.status = status;
    }

    /** Sets the status of notebook. */
    public UserInstanceDTO withStatus(String status) {
        setStatus(status);
        return this;
    }

    /** Returns the name of notebook shape. */
    public String getShape() {
        return shape;
    }

    /** Sets the name of notebook shape. */
    public void setShape(String shape) {
        this.shape = shape;
    }

    /** Sets the name of notebook shape. */
    public UserInstanceDTO withShape(String shape) {
        setShape(shape);
        return this;
    }

    /** Returns the URL of exploratory. */
    public List<ExploratoryURL> getExploratoryUrl() {
        return exploratoryUrl;
    }

    /** Sets the URL of exploratory. */
    public void setExploratoryUrl(List<ExploratoryURL> exploratoryUrl) {
        this.exploratoryUrl = exploratoryUrl;
    }

    /** Sets the URL of exploratory. */
    public UserInstanceDTO withExploratoryUrl(List<ExploratoryURL> exploratoryUrl) {
        setExploratoryUrl(exploratoryUrl);
        return this;
    }

    /** Returns the date and time when the notebook has created. */
    public Date getUptime() {
        return uptime;
    }

    /** Sets the date and time when the notebook has created. */
    public void setUptime(Date uptime) {
        this.uptime = uptime;
    }

    /** Sets the date and time when the notebook has created. */
    public UserInstanceDTO withUptime(Date uptime) {
        setUptime(uptime);
        return this;
    }

    /** Returns private IP address. */
    public String getPrivateIp() {
        return privateIp;
    }

    /** Sets private IP address. */
    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    /** Sets private IP address. */
    public UserInstanceDTO withPrivateIp(String privateIp) {
        setPrivateIp(privateIp);
        return this;
    }

    /** Returns a list of user's computational resources for notebook. */
    public List<UserComputationalResourceDTO> getResources() {
        return resources;
    }

    /** Sets a list of user's computational resources for notebook. */
    public void setResources(List<UserComputationalResourceDTO> resources) {
        this.resources = resources;
    }

    /** Sets a list of user's computational resources for notebook. */
    public UserInstanceDTO withResources(List<UserComputationalResourceDTO> resources) {
        setResources(resources);
        return this;
    }
    
    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("id", id)
    			.add("user", user)
    			.add("exploratoryId", exploratoryId)
    	        .add("exploratoryName", exploratoryName)
    			.add("templateName", templateName)
    			.add("imageName", imageName)
    			.add("imageVersion", imageVersion)
    	        .add("shape", shape)
    	        .add("status", status)
    	        .add("uptime", uptime)
                .add("privateIp", privateIp)
    	        .add("exploratoryUrl", exploratoryUrl)
    	        .add("resources", resources)
    	        .toString();
    }
}
