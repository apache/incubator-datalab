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

package com.epam.datalab.configuration;

import com.epam.datalab.BillingTool;
import com.epam.datalab.core.AdapterBase;
import com.epam.datalab.core.AdapterBase.Mode;
import com.epam.datalab.core.FilterBase;
import com.epam.datalab.core.ModuleBase;
import com.epam.datalab.core.ModuleData;
import com.epam.datalab.core.parser.ParserBase;
import com.epam.datalab.exceptions.AdapterException;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.mongo.MongoDbConnection;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableList;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Describe configuration for {@link BillingTool}
 */
public class BillingToolConfiguration {

    /**
     * The host name.
     */
    @JsonProperty
    private String host;

    /**
     * The port.
     */
    @JsonProperty
    private int port;

    /**
     * The name of database.
     */
    @JsonProperty
    private String database;

    /**
     * The name of user.
     */
    @JsonProperty
    private String username;

    /**
     * The password.
     */
    @JsonProperty
    private String password;

    @JsonProperty
    private boolean billingEnabled;

    /**
     * Adapter for reading source data.
     */
    @Valid
    @NotNull
    @JsonProperty
    private ImmutableList<AdapterBase> adapterIn;

    /**
     * Adapter for writing converted data.
     */
    @Valid
    @NotNull
    @JsonProperty
    private ImmutableList<AdapterBase> adapterOut;

    /**
     * Parser of source data to common format.
     */
    @Valid
    @NotNull
    @JsonProperty
    private ImmutableList<ParserBase> parser;

    /**
     * Filter for source and converted data.
     */
    @Valid
    @JsonProperty
    private ImmutableList<FilterBase> filter = null;

    /**
     * Logging configuration.
     */
    @Valid
    @JsonProperty
    private LoggingConfigurationFactory logging = null;


    /**
     * Working data of modules.
     */
    @JsonIgnore
    private ModuleData moduleData;

    /**
     * Return the adapter for reading source data.
     */
    public ImmutableList<AdapterBase> getAdapterIn() {
        return adapterIn;
    }

    /**
     * Set the adapter for reading source data.
     */
    public void setAdapterIn(ImmutableList<AdapterBase> adapter) {
        for (AdapterBase a : adapter) {
            a.setMode(Mode.READ);
        }
        this.adapterIn = adapter;
    }

    /**
     * Return the adapter for writing converted data.
     */
    public ImmutableList<AdapterBase> getAdapterOut() {
        return adapterOut;
    }

    /**
     * Set the adapter for writing converted data.
     */
    public void setAdapterOut(ImmutableList<AdapterBase> adapter) {
        for (AdapterBase a : adapter) {
            a.setMode(Mode.WRITE);
        }
        this.adapterOut = adapter;
    }

    /**
     * Return the parser of source data to common format.
     */
    public ImmutableList<ParserBase> getParser() {
        return parser;
    }

    /**
     * Set the parser of source data to common format.
     */
    public void setParser(ImmutableList<ParserBase> parser) {
        this.parser = parser;
    }

    /**
     * Return the filter for source and converted data.
     */
    public ImmutableList<FilterBase> getFilter() {
        return filter;
    }

    /**
     * Set the filter for source and converted data.
     */
    public void setFilter(ImmutableList<FilterBase> filter) {
        this.filter = filter;
    }

    /**
     * Return the logging configuration.
     */
    public LoggingConfigurationFactory getLogging() {
        return logging;
    }

    /**
     * Set the logging configuration.
     */
    public void setLogging(LoggingConfigurationFactory logging) {
        this.logging = logging;
    }


    /**
     * Return the working data of modules.
     */
    @JsonIgnore
    public ModuleData getModuleData() {
        return moduleData;
    }

    /**
     * Check and return module.
     *
     * @param modules    the list of modules.
     * @param name       the name of module.
     * @param isOptional optional module or not.
     * @return module
     * @throws InitializationException
     */
    private <T extends ModuleBase> T getModule(ImmutableList<T> modules, String name, boolean isOptional) throws
            InitializationException {
        T module = (modules != null && modules.size() == 1 ? modules.get(0) : null);
        if (!isOptional && module == null) {
            throw new InitializationException("Invalid configuration for property " + name);
        }
        return module;
    }

    /**
     * Build and return the parser.
     *
     * @return the parser.
     * @throws InitializationException
     */
    public ParserBase build() throws InitializationException {
        ParserBase parserBase = getModule(this.parser, "parser", false);
        AdapterBase in = getModule(adapterIn, "adapterIn", false);
        AdapterBase out = getModule(adapterOut, "adapterOut", false);
        FilterBase f = getModule(filter, "filter", true);

        final MongoDbConnection connection;
        try {
            connection = new MongoDbConnection(host, port, database, username, password);
        } catch (AdapterException e) {
            throw new InitializationException("Cannot configure mongo connection. " + e.getLocalizedMessage(), e);
        }
        moduleData = new ModuleData(connection);

        parserBase.setModuleData(moduleData);
        in.setModuleData(moduleData);
        out.setModuleData(moduleData);
        if (f != null) {
            f.setModuleData(moduleData);
        }

        return parserBase.build(in, out, f);
    }

    public boolean isBillingEnabled() {
        return billingEnabled;
    }

    /**
     * Returns a string representation of the object.
     *
     * @param self the object to generate the string for (typically this), used only for its class name.
     */
    public ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("moduleData", moduleData)
                .add("adapterIn", adapterIn)
                .add("adapterOut", adapterOut)
                .add("filter", filter)
                .add("parser", parser)
                .add("logging", logging)
                .add("billingEnabled", billingEnabled);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .toString();
    }
}
