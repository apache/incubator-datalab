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

package com.epam.datalab.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * The abstract adapter for database.
 * See description of {@link ModuleBase} how to create your own adapter.
 */
public abstract class DBAdapterBase extends AdapterBase {

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


    /**
     * Default constructor for deserialization.
     */
    public DBAdapterBase() {
    }

    /**
     * Instantiate adapter for reading or writing.
     *
     * @param mode the mode of adapter.
     */
    public DBAdapterBase(Mode mode) {
        super(mode);
    }


    @Override
    public boolean isWriteHeader() {
        return false;
    }

    /**
     * Return the host name.
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the host name.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Return the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Return the name of database.
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Set the name of database.
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Return the name of user.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the name of user.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Return the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password.
     */
    public void setPassword(String password) {
        this.password = password;
    }


    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("host", host)
                .add("port", port)
                .add("database", database)
                .add("username", username)
                .add("password", "***");
    }
}
