/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.automation.docker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Bridge {

	@JsonProperty("IPAMConfig")
	private Object ipamConfig;

	@JsonProperty("Links")
	private Object links;

	@JsonProperty("Aliases")
	private Object aliases;

	@JsonProperty("NetworkID")
	private String networkId;

	@JsonProperty("EndpointID")
	private String endpointId;

	@JsonProperty("Gateway")
	private String gateway;

	@JsonProperty("IPAddress")
	private String ipAddress;

	@JsonProperty("IPPrefixLen")
	private int ipPrefixLen;

	@JsonProperty("IPv6Gateway")
	private String ipv6Gateway;

	@JsonProperty("GlobalIPv6Address")
	private String globalIpv6Address;

	@JsonProperty("GlobalIPv6PrefixLen")
	private int globalIpv6PrefixLen;

	@JsonProperty("MacAddress")
	private String macAddress;


	public Object getIpamConfig() {
		return ipamConfig;
	}

	public void setIpamConfig(Object ipamConfig) {
		this.ipamConfig = ipamConfig;
	}

	public Object getLinks() {
		return links;
	}

	public void setLinks(Object links) {
		this.links = links;
	}

	public Object getAliases() {
		return aliases;
	}

	public void setAliases(Object aliases) {
		this.aliases = aliases;
	}

	public String getNetworkId() {
		return networkId;
	}

	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}

	public String getEndpointId() {
		return endpointId;
	}

	public void setEndpointId(String endpointId) {
		this.endpointId = endpointId;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getIpPrefixLen() {
		return ipPrefixLen;
	}

	public void setIpPrefixLen(int ipPrefixLen) {
		this.ipPrefixLen = ipPrefixLen;
	}

	public String getIpv6Gateway() {
		return ipv6Gateway;
	}

	public void setIpv6Gateway(String ipv6Gateway) {
		this.ipv6Gateway = ipv6Gateway;
	}

	public String getGlobalIpv6Address() {
		return globalIpv6Address;
	}

	public void setGlobalIpv6Address(String globalIpv6Address) {
		this.globalIpv6Address = globalIpv6Address;
	}

	public int getGlobalIpv6PrefixLen() {
		return globalIpv6PrefixLen;
	}

	public void setGlobalIpv6PrefixLen(int globalIpv6PrefixLen) {
		this.globalIpv6PrefixLen = globalIpv6PrefixLen;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
}
