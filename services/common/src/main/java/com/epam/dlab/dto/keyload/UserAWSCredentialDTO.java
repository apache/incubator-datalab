/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.dto.keyload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAWSCredentialDTO {
    @JsonProperty
    private String hostname;
    @JsonProperty
    private String ip;
    @JsonProperty("key_name")
    private String keyName;
    @JsonProperty("user_own_bicket_name")
    private String userOwnBicketName;
    @JsonProperty("tunnel_port")
    private String tunnelPort;
    @JsonProperty("socks_port")
    private String socksPort;
    @JsonProperty("notebook_sg")
    private String notebookSg;
    @JsonProperty("notebook_profile")
    private String notebookProfile;
    @JsonProperty("notebook_subnet")
    private String notebookSubnet;
    @JsonProperty("edge_sg")
    private String edgeSG;

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getUserOwnBicketName() {
        return userOwnBicketName;
    }

    public String getTunnelPort() {
        return tunnelPort;
    }

    public String getSocksPort() {
        return socksPort;
    }

    public String getNotebookSg() {
        return notebookSg;
    }

    public String getNotebookProfile() {
        return notebookProfile;
    }

    public String getNotebookSubnet() {
        return notebookSubnet;
    }

    public String getEdgeSG() {
        return edgeSG;
    }
}
