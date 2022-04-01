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

package com.epam.datalab.backendapi.util;

import com.epam.datalab.backendapi.domain.AuditReportLine;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

/**
 * Class helper to handle the audit records
 */
public class AuditUtils {

    private static final String REPORT_FIRST_LINE = "Available reporting period from: %s to: %s";
    private static final String[] AUDIT_REPORT_HEADER = {"Date", "User", "Action", "Project", "Resource type", "Resource"};

    /**
     * @param from   formatted date, like 2020-04-07
     * @param to     formatted date, like 2020-05-07
     * @param locale user's locale
     * @return line, like:
     * Available reporting period from: 2020-04-07 to: 2020-04-07"
     */
    public static String getFirstLine(LocalDate from, LocalDate to, String locale) {
        return CSVFormatter.formatLine(Lists.newArrayList(String.format(REPORT_FIRST_LINE,
                        Optional.ofNullable(from).map(date -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(locale)))).orElse(StringUtils.EMPTY),
                        Optional.ofNullable(to).map(date -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(locale)))).orElse(StringUtils.EMPTY))),
                CSVFormatter.SEPARATOR, '\"');
    }

    /**
     *
     * @param line - Audit Report line objet, which contains exported rows
     * @return formatted string of the Audit reportL
     * Date,User,Action,Project,Resource type,Resource
     * 2022-03-20,test,LOG_IN,,,
     * 2022-03-20,test,LOG_IN,,,
     * 2022-03-19,test,LOG_IN,,,
     * 2022-03-17,test,LOG_IN,,,
     */
    public static String printLine(AuditReportLine line) {
        List<String> lines = new ArrayList<>();
        lines.add(getOrEmpty(line.getTimestamp().toString()));
        lines.add(getOrEmpty(line.getUser()));
        lines.add(getOrEmpty(line.getAction()));
        lines.add(getOrEmpty(line.getProject()));
        lines.add(getOrEmpty(line.getResourceType()));;
        lines.add(getOrEmpty(line.getResourceName()));
        return CSVFormatter.formatLine(lines, CSVFormatter.SEPARATOR);
    }

    public static String getHeader() {
        return CSVFormatter.formatLine(Arrays.asList(AuditUtils.AUDIT_REPORT_HEADER), CSVFormatter.SEPARATOR);
    }

    private static String getOrEmpty(String s) {
        return Objects.nonNull(s) ? s : StringUtils.EMPTY;
    }
}
