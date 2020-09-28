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

package com.epam.datalab.module;

import com.epam.datalab.core.parser.ParserByLine;
import com.epam.datalab.exceptions.AdapterException;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.exceptions.ParseException;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse CSV format to common CSV format.
 */
@JsonTypeName(ModuleName.PARSER_CSV)
@JsonClassDescription(
        "CSV parser.\n" +
                "Parse source CSV format to common billing report.\n" +
                "  - type: " + ModuleName.PARSER_CSV + "\n" +
                "    [dataFile: <filename>]           - the file name to store working data of parser.]\n" +
                "    [columnStartDate: <column_name>] - the name of source column with date of data.]\n" +
                "    [columnMapping: >-\n" +
                "                    <targetColumn1=sourceColumnX;targetColumn2=sourceColumnY; ...;\n" +
                "                     tags=sourceColumnK,...,sourceColumnN>]\n" +
                "                                  - columns mapping to target from source columns.\n" +
                "                                    Know target columns: datalab_id, user,\n" +
                "                                    usage_date, product, usage_type, usage, cost,\n" +
                "                                    currency_code, resource_id, tags.\n" +
                "    [whereCondition: >-\n" +
                "                    <(source_columnX > 0.0 || source_columnY == 'string') &&\n" +
                "                     source_columnZ != 2016>]\n" +
                "                                  - where condition for filtering the source data,\n" +
                "                                    see http://commons.apache.org/proper/commons-jexl/reference/syntax.html#Operators\n" +
                "                                    for detais.\n" +
                "    [aggregate: <none | month | day>] - how to aggregate the data.\n" +
                "    [headerLineNo: <number>]          - the number of header line in source data.\n" +
                "    [skipLines: <numbber>]            - the number of line which will be skipped\n" +
                "                                        (include header).\n" +
                "    [fieldSeparator: <char>]          - char for separate field names and values.\n" +
                "    [fieldTerminator: <char>]         - char for terminate field names and values.\n" +
                "    [escapeChar: <char>]              - escape char.\n" +
                "    [decimalSeparator: <char>]        - char for decimal sign.\n" +
                "    [groupingSeparator: <char>]       - char for thousands separator.\n"
)
public class ParserCsv extends ParserByLine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParserCsv.class);

    /**
     * Character for separate field names and values.
     */
    public static final char FIELD_SEPARATOR_DEFAULT = ',';

    /**
     * Character for termination field names and values.
     */
    public static final char FIELD_DELIMITER_DEFAULT = '"';

    /**
     * Escape character.
     */
    public static final char ESCAPE_CHAR_DEFAULT = '\\';


    /**
     * Character for separate field names and values.
     */
    @NotNull
    @JsonProperty
    private char fieldSeparator = FIELD_SEPARATOR_DEFAULT;

    /**
     * Character for termination field names and values.
     */
    @NotNull
    @JsonProperty
    private char fieldTerminator = FIELD_DELIMITER_DEFAULT;

    /**
     * Escape character.
     */
    @NotNull
    @JsonProperty
    private char escapeChar = ESCAPE_CHAR_DEFAULT;

    /**
     * The number of line that contain the header of data.
     */
    @JsonProperty
    private int headerLineNo = 0;

    /**
     * The number of line which will be skipped (include header).
     */
    @JsonProperty
    private int skipLines = 0;


    /**
     * Return the character for separate field names and values.
     */
    public char getFieldSeparator() {
        return fieldSeparator;
    }

    /**
     * Set the character for separate field names and values.
     */
    public void setFieldSeparator(char fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
    }

    /**
     * Return the character for termination field names and values.
     */
    public char getFieldTerminator() {
        return fieldTerminator;
    }

    /**
     * Set the character for termination field names and values.
     */
    public void setFieldTerminator(char fieldTerminator) {
        this.fieldTerminator = fieldTerminator;
    }

    /**
     * Return the escape character.
     */
    public char getEscapeChar() {
        return escapeChar;
    }

    /**
     * Set the escape character.
     */
    public void setEscapeChar(char escapeChar) {
        this.escapeChar = escapeChar;
    }

    /**
     * Return the number of line that contain the header of data.
     */
    public int getHeaderLineNo() {
        return headerLineNo;
    }

    /**
     * Set the number of line that contain the header of data.
     */
    public void setHeaderLineNo(int headerLineNo) {
        this.headerLineNo = headerLineNo;
    }

    /**
     * Return the number of line which will be skipped (include header).
     */
    public int getSkipLines() {
        return skipLines;
    }

    /**
     * Set the number of line which will be skipped (include header).
     */
    public void setSkipLines(int skipLines) {
        this.skipLines = skipLines;
    }


    @Override
    public void initialize() throws InitializationException {
    }

    @Override
    public List<String> parseHeader() throws AdapterException, ParseException {
        String line = null;
        List<String> header = null;

        if (headerLineNo > 0) {
            while (getCurrentStatistics().getRowReaded() < headerLineNo) {
                if ((line = getNextRow()) == null) {
                    return null;
                }
                getCurrentStatistics().incrRowSkipped();
            }
            header = parseRow(line);
        }

        while (getCurrentStatistics().getRowReaded() < skipLines) {
            if (getNextRow() == null) {
                break;
            }
            getCurrentStatistics().incrRowSkipped();
        }

        return header;
    }


    /**
     * Construct the exception.
     *
     * @param message    the error message.
     * @param pos        the position in the parsed line.
     * @param sourceLine the parsed line.
     * @return ParseException
     */
    private ParseException getParseException(String message, int pos, String sourceLine) {
        String s = String.format("%s at pos %d in line: ", message, pos);
        LOGGER.error(s + sourceLine);
        LOGGER.error(StringUtils.repeat(' ', s.length() + pos - 1) + '^');
        return new ParseException(s + sourceLine);
    }

    @Override
    public List<String> parseRow(String line) throws ParseException {
        int realPos = 0;
        int pos = 0;
        boolean isDelimiter = false;
        StringBuilder sb = new StringBuilder(line);
        List<String> row = new ArrayList<>();

        while (pos < sb.length()) {
            char c = sb.charAt(pos);
            if (c == escapeChar) {
                realPos++;
                pos++;
                if (pos == sb.length()) {
                    throw getParseException("Invalid escape char", realPos, line);
                }
                sb.delete(pos - 1, pos);
                realPos++;
            } else if (c == fieldTerminator) {
                realPos++;
                if (isDelimiter) {
                    realPos++;
                    pos++;
                    if (pos == sb.length()) {
                        sb.delete(pos - 1, pos);
                        break;
                    }
                    if (sb.charAt(pos) == fieldSeparator) {
                        row.add(sb.substring(0, pos - 1));
                        sb.delete(0, pos + 1);
                        pos = 0;
                        isDelimiter = false;
                        continue;
                    }
                    throw getParseException("Invalid field delimiter", realPos, line);
                }

                if (pos != 0) {
                    throw getParseException("Unterminated field", realPos, line);
                }
                sb.delete(0, 1);
                isDelimiter = true;
                continue;
            } else if (c == fieldSeparator) {
                realPos++;
                if (isDelimiter) {
                    pos++;
                    continue;
                }
                row.add(sb.substring(0, pos));
                sb.delete(0, pos + 1);
                pos = 0;
            } else {
                realPos++;
                pos++;
            }
        }
        row.add(sb.toString());

        return row;
    }


    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("fieldSeparator", fieldSeparator)
                .add("fieldTerminator", fieldTerminator)
                .add("escapeChar", escapeChar)
                .add("headerLineNo", headerLineNo)
                .add("skipLines", skipLines);
    }
}
