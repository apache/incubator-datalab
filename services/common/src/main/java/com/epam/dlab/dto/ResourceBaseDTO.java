/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

abstract public class ResourceBaseDTO<T extends ResourceBaseDTO<?>> {
    @JsonProperty("conf_service_base_name")
    private String serviceBaseName;
    @JsonProperty("creds_region")
    private String region;

    public String getServiceBaseName() {
        return serviceBaseName;
    }

    public void setServiceBaseName(String serviceBaseName) {
        this.serviceBaseName = serviceBaseName;
    }

    @SuppressWarnings("unchecked")
    public T withServiceBaseName(String serviceBaseName) {
        setServiceBaseName(serviceBaseName);
        return (T) this;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @SuppressWarnings("unchecked")
    public T withRegion(String region) {
        setRegion(region);
        return (T) this;
    }
}
