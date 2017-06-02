package com.epam.dlab.automation.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Bridge {
    
    @JsonProperty("IPAMConfig")
    private Object IPAMConfig;
    
    @JsonProperty("Links")
    private Object Links;
    
    @JsonProperty("Aliases")
    private Object Aliases;
    
    @JsonProperty("NetworkID")
    private String NetworkID;
    
    @JsonProperty("EndpointID")
    private String EndpointID;
    
    @JsonProperty("Gateway")
    private String Gateway;
    
    @JsonProperty("IPAddress")
    private String IPAddress;
    
    @JsonProperty("IPPrefixLen")
    private int IPPrefixLen;
    
    @JsonProperty("IPv6Gateway")
    private String IPv6Gateway;
    
    @JsonProperty("GlobalIPv6Address")
    private String GlobalIPv6Address;
    
    @JsonProperty("GlobalIPv6PrefixLen")
    private int GlobalIPv6PrefixLen;
    
    @JsonProperty("MacAddress")
    private String MacAddress;
    
    public Object getIPAMConfig() {
        return IPAMConfig;
    }
    public void setIPAMConfig(Object iPAMConfig) {
        IPAMConfig = iPAMConfig;
    }
    public Object getLinks() {
        return Links;
    }
    public void setLinks(Object links) {
        Links = links;
    }
    public Object getAliases() {
        return Aliases;
    }
    public void setAliases(Object aliases) {
        Aliases = aliases;
    }
    public String getNetworkID() {
        return NetworkID;
    }
    public void setNetworkID(String networkID) {
        NetworkID = networkID;
    }
    public String getEndpointID() {
        return EndpointID;
    }
    public void setEndpointID(String endpointID) {
        EndpointID = endpointID;
    }
    public String getGateway() {
        return Gateway;
    }
    public void setGateway(String gateway) {
        Gateway = gateway;
    }
    public String getIPAddress() {
        return IPAddress;
    }
    public void setIPAddress(String iPAddress) {
        IPAddress = iPAddress;
    }
    public int getIPPrefixLen() {
        return IPPrefixLen;
    }
    public void setIPPrefixLen(int iPPrefixLen) {
        IPPrefixLen = iPPrefixLen;
    }
    public String getIPv6Gateway() {
        return IPv6Gateway;
    }
    public void setIPv6Gateway(String iPv6Gateway) {
        IPv6Gateway = iPv6Gateway;
    }
    public String getGlobalIPv6Address() {
        return GlobalIPv6Address;
    }
    public void setGlobalIPv6Address(String globalIPv6Address) {
        GlobalIPv6Address = globalIPv6Address;
    }
    public int getGlobalIPv6PrefixLen() {
        return GlobalIPv6PrefixLen;
    }
    public void setGlobalIPv6PrefixLen(int globalIPv6PrefixLen) {
        GlobalIPv6PrefixLen = globalIPv6PrefixLen;
    }
    public String getMacAddress() {
        return MacAddress;
    }
    public void setMacAddress(String macAddress) {
        MacAddress = macAddress;
    }

}
