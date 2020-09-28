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

package com.epam.datalab.backendapi.interceptor;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.annotation.Project;
import com.epam.datalab.backendapi.annotation.User;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceQuoteReachedException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
public class ProjectAdminInterceptor implements MethodInterceptor {
    @Inject
    private ProjectService projectService;

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (grantAccess(mi)) {
            return mi.proceed();
        } else {
            final Method method = mi.getMethod();
            log.warn("Execution of method {} failed because user doesn't have appropriate permission", method.getName());
            throw new ResourceQuoteReachedException("Operation can not be finished. User doesn't have appropriate permission");
        }
    }

    private boolean grantAccess(MethodInvocation mi) {
        final Parameter[] parameters = mi.getMethod().getParameters();
        String project = IntStream.range(0, parameters.length)
                .filter(i -> Objects.nonNull(parameters[i].getAnnotation(Project.class)))
                .mapToObj(i -> (String) mi.getArguments()[i])
                .findAny()
                .orElseThrow(() -> new DatalabException("Project parameter wanted!"));
        UserInfo userInfo = IntStream.range(0, parameters.length)
                .filter(i -> Objects.nonNull(parameters[i].getAnnotation(User.class)))
                .mapToObj(i -> (UserInfo) mi.getArguments()[i])
                .findAny()
                .orElseThrow(() -> new DatalabException("UserInfo parameter wanted!"));

        return checkPermission(userInfo, project);
    }

    private boolean checkPermission(UserInfo userInfo, String project) {
        return UserRoles.isAdmin(userInfo) || UserRoles.isProjectAdmin(userInfo, projectService.get(project).getGroups());
    }
}
