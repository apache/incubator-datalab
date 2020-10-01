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
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class ColumnMetaTest {

    @Test
    public void getColumnIndexByName() throws InitializationException {
        final List<String> columns = Lists.newArrayList("sCol1", "sCol2", "sCol3", "sCol4");

        assertEquals(0, ColumnMeta.getColumnIndexByName("sCol1", columns));
        assertEquals(1, ColumnMeta.getColumnIndexByName("sCol2", columns));
        assertEquals(3, ColumnMeta.getColumnIndexByName("sCol4", columns));

        try {
            ColumnMeta.getColumnIndexByName("NotFound", columns);

            throw new RuntimeException("Test failed");
        } catch (InitializationException e) {
            // OK
        }
    }

    private void checkMapping(ColumnInfo info, String targetColumnName, String sourceColumnName, int
            sourceColumnIndex) {
        assertEquals(targetColumnName, info.targetName);
        assertEquals(sourceColumnName, info.sourceName);
        assertEquals(sourceColumnIndex, info.sourceIndex);
    }

    @Test
    public void create() throws InitializationException {
        final String mapping =
                ColumnMeta.COLUMN_NAMES[0] + "=sCol2;" +
                        ColumnMeta.COLUMN_NAMES[1] + "=$1;" +
                        ReportLine.FIELD_TAGS + "=sCol4,$3";
        final List<String> columns = Lists.newArrayList("sCol1", "sCol2", "sCol3", "sCol4");
        ColumnMeta meta = new ColumnMeta(mapping, columns);

        assertEquals(4, meta.getSourceColumnNames().size());
        assertEquals("sCol1", meta.getSourceColumnNames().get(0));
        assertEquals("sCol2", meta.getSourceColumnNames().get(1));
        assertEquals("sCol3", meta.getSourceColumnNames().get(2));
        assertEquals("sCol4", meta.getSourceColumnNames().get(3));

        assertEquals(ColumnMeta.COLUMN_NAMES.length + 1, meta.getTargetColumnNames().size());
        assertEquals(ColumnMeta.COLUMN_NAMES[0], meta.getTargetColumnNames().get(0));
        assertEquals("sCol4", meta.getTargetColumnNames().get(ColumnMeta.COLUMN_NAMES.length - 1));
        assertEquals("sCol3", meta.getTargetColumnNames().get(ColumnMeta.COLUMN_NAMES.length));

        assertEquals(ColumnMeta.COLUMN_NAMES.length + 1, meta.getColumnMapping().size());
        checkMapping(meta.getColumnMapping().get(0), ColumnMeta.COLUMN_NAMES[0], "sCol2", 1);
        checkMapping(meta.getColumnMapping().get(1), ColumnMeta.COLUMN_NAMES[1], "sCol1", 0);
        checkMapping(meta.getColumnMapping().get(ColumnMeta.COLUMN_NAMES.length - 1), ColumnMeta
                .COLUMN_NAMES[ColumnMeta.COLUMN_NAMES.length - 1], "sCol4", 3);
        checkMapping(meta.getColumnMapping().get(ColumnMeta.COLUMN_NAMES.length), ColumnMeta.COLUMN_NAMES[ColumnMeta
                .COLUMN_NAMES.length - 1], "sCol3", 2);
    }

}
