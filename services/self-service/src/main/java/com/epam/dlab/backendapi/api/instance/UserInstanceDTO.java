/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.api.instance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInstanceDTO {
    @JsonProperty("_id")
    private String id;
    @JsonProperty
    private String user;
    @JsonProperty("exploratory_name")
    private String exploratoryName;
    @JsonProperty
    private String status;
    @JsonProperty
    private String shape;
    @JsonProperty
    private String url;
    @JsonProperty("up_time_since")
    private String upTimeSince;
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

    public String getUpTimeSince() {
        return upTimeSince;
    }

    public void setUpTimeSince(String upTimeSince) {
        this.upTimeSince = upTimeSince;
    }

    public UserInstanceDTO withUpTimeSince(String upTimeSince) {
        setUpTimeSince(upTimeSince);
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
