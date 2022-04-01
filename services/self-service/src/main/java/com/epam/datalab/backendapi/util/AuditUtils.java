package com.epam.datalab.backendapi.util;

import com.epam.datalab.backendapi.domain.AuditReportLine;
import com.epam.datalab.backendapi.domain.BillingReportLine;
import com.epam.datalab.dto.UserInstanceStatus;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

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