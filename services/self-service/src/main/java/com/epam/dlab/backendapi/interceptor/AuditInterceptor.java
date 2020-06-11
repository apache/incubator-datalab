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

package com.epam.dlab.backendapi.interceptor;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.annotation.Audit;
import com.epam.dlab.backendapi.annotation.Info;
import com.epam.dlab.backendapi.annotation.Project;
import com.epam.dlab.backendapi.annotation.ResourceName;
import com.epam.dlab.backendapi.annotation.User;
import com.epam.dlab.backendapi.domain.AuditActionEnum;
import com.epam.dlab.backendapi.domain.AuditDTO;
import com.epam.dlab.backendapi.service.AuditService;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
public class AuditInterceptor implements MethodInterceptor {
    @Inject
    private AuditService auditService;

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        Method method = mi.getMethod();
        final Parameter[] parameters = mi.getMethod().getParameters();
        final String user = getUserInfo(mi, parameters);
        final AuditActionEnum action = getAuditActionEnum(method);
        final String project = getProject(mi, parameters);
        final String resourceName = getResourceName(mi, parameters);
        final List<String> infoMap = getInfo(mi, parameters);

        AuditDTO auditCreateDTO = AuditDTO.builder()
                .user(user)
                .action(action)
                .project(project)
                .resourceName(resourceName)
                .info(infoMap)
                .build();
        auditService.save(auditCreateDTO);
        return mi.proceed();
    }

    private String getUserInfo(MethodInvocation mi, Parameter[] parameters) {
        return IntStream.range(0, parameters.length)
                .filter(i -> Objects.nonNull(parameters[i].getAnnotation(User.class)))
                .mapToObj(i -> ((UserInfo) mi.getArguments()[i]).getName())
                .findAny()
                .orElseThrow(() -> new DlabException("UserInfo parameter wanted!"));
    }

    private AuditActionEnum getAuditActionEnum(Method method) {
        Annotation[] declaredAnnotations = method.getDeclaredAnnotations();
        return IntStream.range(0, method.getDeclaredAnnotations().length)
                .filter(i -> declaredAnnotations[i] instanceof Audit)
                .mapToObj(i -> ((Audit) declaredAnnotations[i]).action())
                .findAny()
                .orElseThrow(() -> new DlabException("'Audit' annotation wanted!"));
    }

    private String getProject(MethodInvocation mi, Parameter[] parameters) {
        return IntStream.range(0, parameters.length)
                .filter(i -> Objects.nonNull(parameters[i].getAnnotation(Project.class)))
                .mapToObj(i -> (String) mi.getArguments()[i])
                .findAny()
                .orElse(StringUtils.EMPTY);
    }

    private String getResourceName(MethodInvocation mi, Parameter[] parameters) {
        return IntStream.range(0, parameters.length)
                .filter(i -> Objects.nonNull(parameters[i].getAnnotation(ResourceName.class)))
                .mapToObj(i -> (String) mi.getArguments()[i])
                .findAny()
                .orElseThrow(() -> new DlabException("Resource name parameter wanted!"));
    }

    private List<String> getInfo(MethodInvocation mi, Parameter[] parameters) {
        return IntStream.range(0, parameters.length)
                .filter(i -> Objects.nonNull(parameters[i].getAnnotation(Info.class)) && Objects.nonNull(mi.getArguments()[i]))
                .mapToObj(i -> (List<String>) mi.getArguments()[i])
                .findAny()
                .orElseGet(Collections::emptyList);
    }
}
