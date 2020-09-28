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

package com.epam.datalab.module.aws;

import com.epam.datalab.core.FilterBase;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.model.aws.ReportLine;
import com.epam.datalab.module.ModuleName;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects.ToStringHelper;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Filter and transform the line of AWS detailed billing reports.
 */
@JsonTypeName(ModuleName.FILTER_AWS)
@JsonClassDescription(
        "Amazon Web Services detailed billing reports filter.\n" +
                "Filter report data and select line item only. Set column projectCode and\n" +
                "currencyCode to user values.\n" +
                "  - type: " + ModuleName.FILTER_AWS + "\n" +
                "    [currencyCode: <string>]    - user value for currencyCode column.\n" +
                "    [columnDatalabTag: <string>]   - name of column tag of DataLab resource id.\n" +
                "    [serviceBaseName: <string>] - DataLab's service base name."

)
public class FilterAWS extends FilterBase {

    /**
     * The code of currency.
     */
    @NotNull
    @JsonProperty
    private String currencyCode;

    /**
     * Name of report column tag of DataLab.
     */
    @NotNull
    @JsonProperty
    private String columnDatalabTag;

    /**
     * DataLab service base name.
     */
    @NotNull
    @JsonProperty
    private String serviceBaseName;
    private int datalabIdIndex = -1;
    private String datalabPrefix;

    /**
     * Return the code of currency for billing.
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    /**
     * Set the code of currency for billing.
     */
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /**
     * Return the name of report column tag of DataLab.
     */
    public String getColumnDatalabTag() {
        return columnDatalabTag;
    }

    /**
     * Set the name of report column tag of DataLab.
     */
    public void setDatalabTagName(String columnDatalabTag) {
        this.columnDatalabTag = columnDatalabTag;
    }

    /**
     * Return service base name.
     */
    public String getServiceBaseName() {
        return serviceBaseName;
    }

    /**
     * Set service base name.
     */
    public void setServiceBaseName(String serviceBaseName) {
        this.serviceBaseName = serviceBaseName;
    }

    @Override
    public void initialize() throws InitializationException {
        datalabIdIndex = (getColumnDatalabTag() == null ? -1 :
                getParser().getSourceColumnIndexByName(getColumnDatalabTag()));
        datalabPrefix = getServiceBaseName() + ":";
    }

    @Override
    public String canParse(String line) {
        return line;
    }

    @Override
    public List<String> canTransform(List<String> row) {
        if (datalabIdIndex != -1 &&
                (row.size() <= datalabIdIndex ||
                        !row.get(datalabIdIndex).startsWith(datalabPrefix))) {
            return null;
        }
        return row;
    }

    @Override
    public ReportLine canAccept(ReportLine row) {
        row.setCurrencyCode(currencyCode);
        return row;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("currencyCode", currencyCode)
                .add("columnDatalabTag", columnDatalabTag)
                .add("serviceBaseName", serviceBaseName);
    }
}
