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

package com.epam.dlab.automation.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DockerContainer {
    
    @JsonProperty
    private String Id;
    
    @JsonProperty
    private List<String> Names;
    
    @JsonProperty
    private String Image;
    
    @JsonProperty
    private String ImageID;
    
    @JsonProperty
    private String Command;
    
    @JsonProperty
    private int Created;
    
    @JsonProperty
    private List<Object> Ports;
    
    @JsonProperty
    private Labels Labels;
    
    @JsonProperty
    private String State;
    
    @JsonProperty 
    private String Status;
    
    @JsonProperty
    private HostConfig HostConfig;
    
    @JsonProperty
    private NetworkSettings NetworkSettings;
    
    @JsonProperty
    private List<Object> Mounts;
    
    public String getId() {
        return Id;
    }
    public void setId(String Id) {
        this.Id = Id;
    }
    public List<String> getNames() {
        return Names;
    }
    public void setNames(List<String> names) {
        Names = names;
    }
    public String getImage() {
        return Image;
    }
    public void setImage(String image) {
        Image = image;
    }
    public String getImageID() {
        return ImageID;
    }
    public void setImageID(String imageID) {
        ImageID = imageID;
    }
    public String getCommand() {
        return Command;
    }
    public void setCommand(String command) {
        Command = command;
    }
    public int getCreated() {
        return Created;
    }
    public void setCreated(int created) {
        Created = created;
    }
    public List<Object> getPorts() {
        return Ports;
    }
    public void setPorts(List<Object> ports) {
        Ports = ports;
    }
    public Labels getLabels() {
        return Labels;
    }
    public void setLabels(Labels labels) {
        Labels = labels;
    }
    public String getState() {
        return State;
    }
    public void setState(String state) {
        State = state;
    }
    public String getStatus() {
        return Status;
    }
    public void setStatus(String status) {
        Status = status;
    }
    public HostConfig getHostConfig() {
        return HostConfig;
    }
    public void setHostConfig(HostConfig hostConfig) {
        HostConfig = hostConfig;
    }
    public NetworkSettings getNetworkSettings() {
        return NetworkSettings;
    }
    public void setNetworkSettings(NetworkSettings networkSettings) {
        NetworkSettings = networkSettings;
    }
    public List<Object> getMounts() {
        return Mounts;
    }
    public void setMounts(List<Object> mounts) {
        Mounts = mounts;
    }
}
