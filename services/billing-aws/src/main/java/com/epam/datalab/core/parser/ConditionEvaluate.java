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
import com.epam.datalab.exceptions.ParseException;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.internal.Script;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluate condition for filtering source data.
 */
public class ConditionEvaluate {

    /**
     * Names of columns for condition.
     */
    private String[] columnNames;

    /**
     * Indexes of columns for condition.
     */
    private int[] columnIndexes;

    /**
     * JEXL expression for evaluation.
     */
    private final Script expression;

    /**
     * JEXL context to evaluate row.
     */
    private final JexlContext jexlContext;

    /**
     * Instantiate the engine to evaluate condition.
     *
     * @param columnNames the list of column names.
     * @param condition   condition for filtering data.
     * @throws InitializationException
     */
    public ConditionEvaluate(List<String> columnNames, String condition) throws InitializationException {
        //Replace : to . in column names
        List<String> colNames = new ArrayList<>(columnNames.size());
        for (int i = 0; i < columnNames.size(); i++) {
            String name = columnNames.get(i);
            if (name.indexOf(':') > -1 && condition.indexOf(name) > -1) {
                String newName = StringUtils.replaceChars(name, ':', '.');
                colNames.add(newName);
                condition = StringUtils.replace(condition, name, newName);
            } else {
                colNames.add(name);
            }
        }

        try {
            JexlEngine engine = new JexlBuilder().strict(true).silent(false).debug(true).create();
            expression = (Script) engine.createExpression(condition);
            jexlContext = new MapContext();
        } catch (Exception e) {
            throw new InitializationException("Cannot initialize JEXL engine for condition: " + condition + ". " +
                    e.getLocalizedMessage(), e);
        }

        // Create mapping of columns for evaluations.
        List<String> names = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        for (List<String> variableList : expression.getVariables()) {
            String columnName = StringUtils.join(variableList, '.');
            int index = getColumnIndex(colNames, columnName);
            if (index == -1) {
                throw new InitializationException("Unknow source column name \"" + columnName + "\" in condition: " +
                        expression.getSourceText() + ". Known column names: " + StringUtils.join(columnNames, ", ") + ".");
            }
            names.add(columnName);
            indexes.add(index);
        }

        this.columnNames = new String[names.size()];
        this.columnIndexes = new int[indexes.size()];
        for (int i = 0; i < indexes.size(); i++) {
            this.columnNames[i] = names.get(i);
            this.columnIndexes[i] = indexes.get(i);
        }
    }

    /**
     * Find and return the index of column in the given column list.
     *
     * @param columnNames the list of column names.
     * @param columnName  the name of column to find.
     */
    private int getColumnIndex(List<String> columnNames, String columnName) {
        for (int i = 0; i < columnNames.size(); i++) {
            if (columnName.equals(columnNames.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Evaluate condition for given row.
     *
     * @param row the row to evaluate.
     * @return <true> if condition is true.
     * @throws ParseException if condition is not return boolean type.
     */
    public boolean evaluate(List<String> row) throws ParseException {
        for (int i = 0; i < columnNames.length; i++) {
            jexlContext.set(columnNames[i], row.get(columnIndexes[i]));
        }
        Object value;
        try {
            value = expression.evaluate(jexlContext);
        } catch (Exception e) {
            throw new ParseException("Cannot evaluate condition: " + expression.getSourceText() + ". " +
                    e.getLocalizedMessage(), e);
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new ParseException("Invalid condition: " + expression.getSourceText());
    }


    /**
     * Returns a string representation of the object.
     *
     * @param self the object to generate the string for (typically this), used only for its class name.
     */
    public ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("columnNames", columnNames)
                .add("columnIndexes", columnIndexes);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
