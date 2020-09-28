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

import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.model.aws.ReportLine;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides column meta information.
 */
public class ColumnMeta {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColumnMeta.class);

    /**
     * Character for separate the tag values and column names.
     */
    public static final char TAG_SEPARATOR = ',';
    /**
     * Character for separate the column mapping.
     */
    public static final char MAPPING_COLUMN_SEPARATOR = ';';

    /**
     * The column names for common format.
     */
    static final String[] COLUMN_NAMES = {
            ReportLine.FIELD_DATALAB_ID,
            ReportLine.FIELD_USER_ID,
            ReportLine.FIELD_USAGE_DATE,
            ReportLine.FIELD_PRODUCT,
            ReportLine.FIELD_USAGE_TYPE,
            ReportLine.FIELD_USAGE,
            ReportLine.FIELD_COST,
            ReportLine.FIELD_CURRENCY_CODE,
            ReportLine.FIELD_RESOURCE_ID,
            ReportLine.FIELD_TAGS
    };

    /**
     * The index of the first column for tags.
     */
    public static final int TAG_COLUMN_INDEX = COLUMN_NAMES.length - 1;

    /**
     * The list of target column names.
     */
    private List<String> targetColumnNames;

    /**
     * The list of source column names.
     */
    private final List<String> sourceColumnNames;

    /**
     * The list of column mapping: source to target.
     */
    private List<ColumnInfo> columnMapping;


    /**
     * Instantiate the common format parser. <b>columnMappingString</b> is semicolon separated
     * string with key=value as target=source columns name or indexes of source column. For example,<br>
     * "accountId=PayerAccountId;usageIntervalStart=UsageStartDate;usageIntervalEnd=UsageEndDate; ...
     * ;tags=user:tag1,user:tag2,user:tagN".
     *
     * @param columnMappingString column mapping: source to target. if <b>null</b>
     *                            the source data will be converted without mapping.
     * @param sourceColumnNames   the source column names.
     * @throws InitializationException
     */
    public ColumnMeta(String columnMappingString, List<String> sourceColumnNames) throws InitializationException {
        this.sourceColumnNames = sourceColumnNames;
        if (columnMappingString != null) {
            try {
                setColumnMapping(columnMappingString, sourceColumnNames);
            } catch (Exception e) {
                throw new InitializationException("Column mapping error. " + e.getLocalizedMessage(), e);
            }
        }
    }


    /**
     * Return the list of target column names.
     */
    public List<String> getTargetColumnNames() {
        return targetColumnNames;
    }

    /**
     * Return the list of source column names.
     */
    public List<String> getSourceColumnNames() {
        return sourceColumnNames;
    }

    /**
     * Return the list of column mapping: source to target.
     */
    public List<ColumnInfo> getColumnMapping() {
        return columnMapping;
    }


    /**
     * Return the index of column in the list <b>columnNames</b> or throw exception {@link InitializationException}
     *
     * @param columnName  the name of column.
     * @param columnNames the list of column names.
     * @return the index of column.
     * @throws InitializationException if column not found in the list of columns.
     */
    public static int getColumnIndexByName(String columnName, List<String> columnNames) throws
            InitializationException {
        for (int i = 0; i < columnNames.size(); i++) {
            if (columnName.equals(columnNames.get(i))) {
                return i;
            }
        }
        throw new InitializationException("Column index not detected for column \"" + columnName + "\"");
    }

    /**
     * Return the index of column in the list <b>columnNames</b> or throw exception {@link InitializationException}.
     * columnName may be present as column index. For example like this "$2" for second column.
     *
     * @param columnName  the name of column or index.
     * @param columnNames the list of column names.
     * @return the index of column.
     * @throws InitializationException if column not found in the list of columns.
     */
    private static int getColumnIndex(String columnName, List<String> columnNames) throws InitializationException {
        if (columnName.startsWith("$")) {
            try {
                return Integer.parseInt(columnName.substring(1)) - 1;
            } catch (NumberFormatException e) {
                // Not a column index but column name
            }
        }
        if (columnNames == null) {
            throw new InitializationException("Invalid column index \"" + columnName + "\"");
        }
        return getColumnIndexByName(columnName, columnNames);
    }

    /**
     * Return the index of column in the list <b>columnNames</b> or throw exception {@link InitializationException}.
     * columnName may be present as column index. For example like this "$2" for second column.
     *
     * @param columnName the name of column or index.
     * @return the index of column.
     * @throws InitializationException if column not found in the list of columns.
     */
    private static int getColumnIndex(String columnName) throws InitializationException {
        ArrayList<String> list = new ArrayList<>(COLUMN_NAMES.length);
        for (String s : COLUMN_NAMES) {
            list.add(s);
        }
        return getColumnIndexByName(columnName, list);
    }

    /**
     * Create map of target and source columns for column mapping. Key of map is target column, the value
     * is source column. <b>columnMappingString</b> is semicolon separated string with key=value as
     * target=source columns name or indexes of source column. For example,<br>
     * "accountId=PayerAccountId;usageIntervalStart=UsageStartDate;usageIntervalEnd=UsageEndDate; ...
     * ;tags=user:tag1,user:tag2,user:tagN".
     *
     * @param columnMappingString column mapping: source to target.
     * @param sourceColumnNames
     * @return Map of target and source columns for column mapping.
     * @throws InitializationException
     */
    private Map<String, String> getSourceToTarget(String columnMappingString, List<String> sourceColumnNames) throws
            InitializationException {
        String[] entries = StringUtils.split(columnMappingString, MAPPING_COLUMN_SEPARATOR);
        Map<String, String> sourceToTarget = new HashMap<>();

        for (String entry : entries) {
            if (entry.trim().isEmpty() || !entry.contains("=")) {
                throw new InitializationException("Invalid the entry \"" + entry + "\"in column mapping");
            }
            String[] pair = StringUtils.split(entry, '=');
            if (pair.length != 2) {
                throw new InitializationException("Invalid the entry \"" + entry + "\"in column mapping");
            }

            pair[0] = pair[0].trim();
            pair[1] = pair[1].trim();

            try {
                int index = getColumnIndex(pair[0]);
                pair[0] = COLUMN_NAMES[index];
            } catch (InitializationException e) {
                throw new InitializationException("Unkown target column \"" + pair[0] + "\".", e);
            }

            try {
                if (!pair[0].equals(ReportLine.FIELD_TAGS)) {
                    int index = getColumnIndex(pair[1], sourceColumnNames);
                    if (sourceColumnNames != null) {
                        pair[1] = sourceColumnNames.get(index);
                    }
                }
            } catch (InitializationException e) {
                if (sourceColumnNames == null) {
                    throw new InitializationException("Invalid column index \"" + pair[1] + "\" or column header not " +
                            "defined");
                }
                throw new InitializationException("Unkown source column \"" + pair[1] + "\".", e);
            }
            sourceToTarget.put(pair[0], pair[1]);
        }

        return sourceToTarget;
    }

    /**
     * Initialize and set column mapping. <b>columnMappingString</b> is semicolon separated string with key=value as
     * target=source columns name or indexes of source column. For example,<br>
     * "accountId=PayerAccountId;usageIntervalStart=UsageStartDate;usageIntervalEnd=UsageEndDate; ...
     * ;tags=user:tag1,user:tag2,user:tagN".
     *
     * @param columnMappingString column mapping: source to target. if <b>null</b>
     *                            the source data will be converted without mapping.
     * @param sourceColumnNames   the list of source column names.
     * @throws InitializationException
     */
    private void setColumnMapping(String columnMappingString, List<String> sourceColumnNames) throws
            InitializationException {
        if (columnMappingString == null) {
            throw new InitializationException("Mapping not defined.");
        }

        Map<String, String> sourceToTarget = getSourceToTarget(columnMappingString, sourceColumnNames);
        String tags = sourceToTarget.get(ReportLine.FIELD_TAGS);
        List<String> tagColumns = (tags == null ? null :
                Arrays.asList(StringUtils.split(tags, TAG_SEPARATOR)));

        LOGGER.info("Mapping columns [target=source:name[index]]:");
        int columnCount = COLUMN_NAMES.length - 1;
        int tagCount = (tagColumns == null ? 0 : tagColumns.size());
        columnMapping = new ArrayList<>(columnCount + tagCount);
        targetColumnNames = new ArrayList<>(columnCount + tagCount);

        for (int i = 0; i < columnCount; i++) {
            String sourceName = sourceToTarget.get(COLUMN_NAMES[i]);
            ColumnInfo columnInfo = new ColumnInfo(
                    COLUMN_NAMES[i],
                    sourceName,
                    (sourceName == null ? -1 : getColumnIndex(sourceName, sourceColumnNames)));
            columnMapping.add(columnInfo);
            targetColumnNames.add(COLUMN_NAMES[i]);
            LOGGER.info("  " + columnInfo.toString());
        }

        for (int i = 0; i < tagCount; i++) {
            String sourceName = tagColumns.get(i).trim();
            int sourceIndex = getColumnIndex(sourceName, sourceColumnNames);
            if (sourceColumnNames != null) {
                sourceName = sourceColumnNames.get(sourceIndex);
            }
            ColumnInfo columnInfo = new ColumnInfo(
                    ReportLine.FIELD_TAGS,
                    sourceName,
                    sourceIndex);
            columnMapping.add(columnInfo);
            targetColumnNames.add(sourceName);
            LOGGER.info("  " + columnInfo.toString());
        }
    }


    /**
     * Returns a string representation of the object.
     *
     * @param self the object to generate the string for (typically this), used only for its class name.
     */
    public ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("targetColumnNames", targetColumnNames)
                .add("sourceColumnNames", sourceColumnNames)
                .add("columnMapping", columnMapping);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
