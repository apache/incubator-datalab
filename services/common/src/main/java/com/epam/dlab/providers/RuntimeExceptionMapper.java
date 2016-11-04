/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class RuntimeExceptionMapper extends GenericExceptionMapper<RuntimeException> {
    final static Logger LOGGER = LoggerFactory.getLogger(RuntimeExceptionMapper.class);

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof WebApplicationException) {
            return handleWebApplicationException(exception);
        }
        return super.toResponse(exception);
    }

    private Response handleWebApplicationException(RuntimeException exception) {
        WebApplicationException webAppException = (WebApplicationException) exception;

        if (webAppException.getResponse().getStatusInfo() == Response.Status.UNAUTHORIZED) {
            return web(exception, Response.Status.UNAUTHORIZED);
        }
        if (webAppException.getResponse().getStatusInfo() == Response.Status.NOT_FOUND) {
            return web(exception, Response.Status.NOT_FOUND);
        }

        return super.toResponse(exception);
    }

    private Response web(RuntimeException exception, Response.StatusType status) {
        LOGGER.error("Web application exception: {}", exception.getMessage());
        return Response.status(status).build();
    }
}
