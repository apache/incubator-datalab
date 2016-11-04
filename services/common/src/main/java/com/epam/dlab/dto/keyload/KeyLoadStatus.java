/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.dto.keyload;

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
        return Arrays.stream(values()).reduce(NONE, (result, next) -> next.status.equals(status) ? next : result);
    }
}
