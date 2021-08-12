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
import com.epam.datalab.backendapi.annotation.Audit;
import com.epam.datalab.backendapi.annotation.Info;
import com.epam.datalab.backendapi.annotation.Project;
import com.epam.datalab.backendapi.annotation.ResourceName;
import com.epam.datalab.backendapi.annotation.User;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.domain.AuditActionEnum;
import com.epam.datalab.backendapi.domain.AuditDTO;
import com.epam.datalab.backendapi.domain.AuditResourceTypeEnum;
import com.epam.datalab.backendapi.service.AuditService;
import com.epam.datalab.exceptions.DatalabException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
public class AuditInterceptor implements MethodInterceptor {
    @Inject
    private AuditService auditService;
    @Inject
    private SelfServiceApplicationConfiguration configuration;

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (configuration.isAuditEnabled()) {
            Method method = mi.getMethod();
            final Parameter[] parameters = mi.getMethod().getParameters();
            final String user = getUserInfo(mi, parameters);
            final AuditActionEnum action = getAuditAction(method);
            final AuditResourceTypeEnum resourceType = getResourceType(method);
            final String project = getProject(mi, parameters);
            final String resourceName = getResourceName(mi, parameters);
            final String auditInfo = getInfo(mi, parameters);

            AuditDTO auditCreateDTO = AuditDTO.builder()
                    .user(user)
                    .action(action)
                    .type(resourceType)
                    .project(project)
                    .resourceName(resourceName)
                    .info(auditInfo)
                    .build();
            auditService.save(auditCreateDTO);
        }
        return mi.proceed();
    }

    private String getUserInfo(MethodInvocation mi, Parameter[] parameters) {
        return IntStream.range(0, parameters.length)
                .filter(i -> Objects.nonNull(parameters[i].getAnnotation(User.class)))
                .mapToObj(i -> ((UserInfo) mi.getArguments()[i]).getName())
                .findAny()
                .orElseThrow(() -> new DatalabException("UserInfo parameter wanted!"));
    }

    private AuditActionEnum getAuditAction(Method method) {
        Annotation[] declaredAnnotations = method.getDeclaredAnnotations();
        return IntStream.range(0, method.getDeclaredAnnotations().length)
                .filter(i -> declaredAnnotations[i] instanceof Audit)
                .mapToObj(i -> ((Audit) declaredAnnotations[i]).action())
                .findAny()
                .orElseThrow(() -> new DatalabException("'Audit' annotation wanted!"));
    }

    private AuditResourceTypeEnum getResourceType(Method method) {
        Annotation[] declaredAnnotations = method.getDeclaredAnnotations();
        return IntStream.range(0, method.getDeclaredAnnotations().length)
                .filter(i -> declaredAnnotations[i] instanceof Audit)
                .mapToObj(i -> ((Audit) declaredAnnotations[i]).type())
                .findAny()
                .orElseThrow(() -> new DatalabException("'Audit' annotation wanted!"));
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
                .orElse(StringUtils.EMPTY);
    }

    private String getInfo(MethodInvocation mi, Parameter[] parameters) {
        return IntStream.range(0, parameters.length)
                .filter(i -> Objects.nonNull(parameters[i].getAnnotation(Info.class)) && Objects.nonNull(mi.getArguments()[i]))
                .mapToObj(i -> (String) mi.getArguments()[i])
                .findAny()
                .orElse(StringUtils.EMPTY);
    }
}
