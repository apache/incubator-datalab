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

package com.epam.datalab.core.parser;

import com.epam.datalab.exceptions.ParseException;
import com.epam.datalab.model.aws.ReportLine;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Provides common format features.
 */
public class CommonFormat {
    /**
     * Character for separate field names and values.
     */
    public static final char FIELD_SEPARATOR = ',';

    /**
     * Character for termination field names and values.
     */
    public static final char FIELD_DELIMITER = '"';

    /**
     * Escape character.
     */
    public static final char ESCAPE_CHAR = '\\';

    /**
     * Default character used for decimal sign.
     */
    public static final char DECIMAL_SEPARATOR_DEFAULT = '.';

    /**
     * Default character used for thousands separator.
     */
    public static final char DECIMAL_GROUPING_SEPARATOR_DEFAULT = ' ';

    /**
     * String of the field separator for replacement to target data.
     */
    private static final String DELIMITER_REPLACE_FROM = String.valueOf(FIELD_DELIMITER);

    /**
     * String of the escaped field separator for replacement to target data.
     */
    private static final String DELIMITER_REPLACE_TO = new StringBuilder()
            .append(ESCAPE_CHAR)
            .append(FIELD_DELIMITER)
            .toString();

    /**
     * Formatter for convert decimal numbers to string.
     */
    private static final DecimalFormat DECIMAL_TO_STRING_FORMAT;

    static {
        DECIMAL_TO_STRING_FORMAT = new DecimalFormat();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(DECIMAL_SEPARATOR_DEFAULT);
        DECIMAL_TO_STRING_FORMAT.setDecimalFormatSymbols(symbols);
        DECIMAL_TO_STRING_FORMAT.setGroupingUsed(false);
        DECIMAL_TO_STRING_FORMAT.setMaximumFractionDigits(100);
    }


    /**
     * Column meta information.
     */
    private final ColumnMeta columnMeta;

    /**
     * Formatter for parse of decimal number .
     */
    private final DecimalFormat sourceDecimalFormat;


    /**
     * Instantiate the helper for common format.
     *
     * @param columnMeta column meta information.
     */
    public CommonFormat(ColumnMeta columnMeta) {
        this(columnMeta, DECIMAL_SEPARATOR_DEFAULT, DECIMAL_GROUPING_SEPARATOR_DEFAULT);
    }

    /**
     * Instantiate the helper for common format.
     *
     * @param columnMeta        column meta information.
     * @param decimalSeparator  the character used for decimal sign.
     * @param groupingSeparator the character used for thousands separator.
     */
    public CommonFormat(ColumnMeta columnMeta, char sourceDecimalSeparator, char sourceGroupingSeparator) {
        this.columnMeta = columnMeta;
        this.sourceDecimalFormat = getDecimalFormat(sourceDecimalSeparator, sourceGroupingSeparator);
    }


    /**
     * Create and return the target row for common format from source.
     *
     * @param sourceRow the source row.
     * @return row in common format.
     * @throws ParseException
     */
    public ReportLine toCommonFormat(List<String> sourceRow) throws ParseException {
        if (columnMeta.getColumnMapping() == null) {
            return toReportLine(sourceRow);
        }

        List<String> targetRow = new ArrayList<>();
        for (ColumnInfo columnInfo : columnMeta.getColumnMapping()) {
            targetRow.add((columnInfo.sourceIndex < 0 ? "" :
                    (columnInfo.sourceIndex < sourceRow.size() ? sourceRow.get(columnInfo.sourceIndex) : null)));
        }
        return toReportLine(targetRow);
    }

    /**
     * Add the column value to string in CSV format.
     *
     * @param sb    the buffer of sting.
     * @param value the value.
     * @return the buffer of string.
     */
    private static StringBuilder addToStringBuilder(StringBuilder sb, String value) {
        if (sb.length() > 0) {
            sb.append(FIELD_SEPARATOR);
        }
        return sb.append(FIELD_DELIMITER)
                .append(StringUtils.replace(value, DELIMITER_REPLACE_FROM, DELIMITER_REPLACE_TO))
                .append(FIELD_DELIMITER);
    }

    /**
     * Convert row to line in CSV format.
     *
     * @param row row for convertation.
     * @return string in CSV format.
     */
    public static String rowToString(List<String> row) {
        StringBuilder sb = new StringBuilder();
        for (String s : row) {
            addToStringBuilder(sb, s);
        }
        return sb.toString();
    }

    /**
     * Convert row to line in CSV format.
     *
     * @param row row for convertation.
     * @return string in CSV format.
     */
    public static String rowToString(ReportLine row) {
        StringBuilder sb = new StringBuilder();

        addToStringBuilder(sb, row.getDatalabId());
        addToStringBuilder(sb, row.getUser());
        addToStringBuilder(sb, row.getUsageDate());
        addToStringBuilder(sb, row.getProduct());
        addToStringBuilder(sb, row.getUsageType());
        addToStringBuilder(sb, doubleToString(row.getUsage()));
        addToStringBuilder(sb, doubleToString(row.getCost()));
        addToStringBuilder(sb, row.getCurrencyCode());
        addToStringBuilder(sb, row.getResourceType().toString());
        addToStringBuilder(sb, row.getResourceId());

        if (row.getTags() != null) {
            for (String key : row.getTags().keySet()) {
                addToStringBuilder(sb, row.getTags().get(key));
            }
        }
        return sb.toString();
    }

    /**
     * Create and return decimal formatter.
     *
     * @param decimalSeparator  the character used for decimal sign.
     * @param groupingSeparator the character used for thousands separator.
     * @return Formatter for decimal digits.
     */
    private DecimalFormat getDecimalFormat(char decimalSeparator, char groupingSeparator) {
        DecimalFormat df = new DecimalFormat();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(decimalSeparator);
        symbols.setGroupingSeparator(groupingSeparator);
        df.setDecimalFormatSymbols(symbols);
        return df;
    }

    /**
     * Parse and return double value. If value is <b>null</b> or empty return zero.
     *
     * @param columnName the name of column.
     * @param value      the value.
     * @throws ParseException
     */
    public double parseDouble(String columnName, String value) throws ParseException {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return sourceDecimalFormat.parse(value).doubleValue();
        } catch (Exception e) {
            throw new ParseException("Cannot cast column " + columnName + " value \"" + value + "\" to double: " + e
                    .getLocalizedMessage(), e);
        }
    }

    /**
     * Return the string representation of double value.
     *
     * @param value the value.
     */
    public static String doubleToString(double value) {
        return DECIMAL_TO_STRING_FORMAT.format(value);
    }

    /**
     * Creates and returns the line of billing report from the list of values.
     *
     * @param row the list of values.
     * @return the line of billing report.
     * @throws ParseException
     */
    public ReportLine toReportLine(List<String> row) throws ParseException {
        if (row.size() < ColumnMeta.TAG_COLUMN_INDEX || row.size() > columnMeta.getTargetColumnNames().size()) {
            throw new ParseException("Invalid the number of columns in list: expected from " + ColumnMeta
                    .TAG_COLUMN_INDEX +
                    " to " + columnMeta.getTargetColumnNames().size() + ", actual " + row.size());
        }
        ReportLine line = new ReportLine();
        int i = 0;

        line.setDatalabId(row.get(i));
        line.setUser(row.get(++i));
        line.setUsageDate(row.get(++i));
        line.setProduct(row.get(++i));
        line.setUsageType(row.get(++i));
        line.setUsage(parseDouble("usage", row.get(++i)));
        line.setCost(parseDouble("cost", row.get(++i)));
        line.setCurrencyCode(row.get(++i));
        line.setResourceTypeId(row.get(++i));

        if (row.size() >= ColumnMeta.TAG_COLUMN_INDEX) {
            LinkedHashMap<String, String> tags = new LinkedHashMap<>();
            i++;
            while (i < row.size()) {
                tags.put(columnMeta.getTargetColumnNames().get(i), row.get(i++));
            }
            line.setTags(tags);
        }

        return line;
    }


    /**
     * Print row to console.
     *
     * @param row array of values.
     */
    public static void printRow(String[] row) {
        System.out.print(" | ");
        for (String s : row) {
            System.out.print(s + " | ");
        }
        System.out.println();
    }

    /**
     * Print row to console.
     *
     * @param row list of values.
     */
    public static void printRow(List<String> row) {
        printRow(row.toArray(new String[row.size()]));
    }
}
