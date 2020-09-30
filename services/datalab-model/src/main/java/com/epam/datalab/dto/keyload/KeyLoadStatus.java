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

package com.epam.datalab.dto.keyload;

import javax.ws.rs.core.Response;
import java.util.Arrays;

public enum KeyLoadStatus {
    NONE("none", null, Response.Status.NOT_FOUND),
    NEW("new", null, Response.Status.ACCEPTED),
    SUCCESS("success", "ok", Response.Status.OK),
    ERROR("error", "err", Response.Status.INTERNAL_SERVER_ERROR);

    private String status;
    private String value;
    private Response.Status httpStatus;

    KeyLoadStatus(String status, String value, Response.Status httpStatus) {
        this.status = status;
        this.value = value;
        this.httpStatus = httpStatus;
    }

    public String getStatus() {
        return status;
    }

    public Response.Status getHttpStatus() {
        return httpStatus;
    }

    public static boolean isSuccess(String value) {
        return SUCCESS.value.equals(value);
    }

    public static String getStatus(boolean successed) {
        return successed ? SUCCESS.status : ERROR.status;
    }

    public static KeyLoadStatus findByStatus(String status) {
        return Arrays.stream(values()).reduce(NONE, (result, next) -> next.status.equalsIgnoreCase(status) ? next : result);
    }
}
