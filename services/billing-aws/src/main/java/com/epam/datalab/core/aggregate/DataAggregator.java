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

package com.epam.datalab.core.aggregate;

import com.epam.datalab.model.aws.ReportLine;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * Aggregate billing report and summarizes column usage and cost.
 */
public class DataAggregator {
    /**
     * List of the report lines.
     */
    private final Vector<ReportLine> reportLines = new Vector<>(1000);

    /**
     * Comparator for aggregation.
     */
    private final Comparator<ReportLine> aggComparator = new AggComparator();

    /**
     * Granularity for aggregation.
     */
    private AggregateGranularity granularity;

    /**
     * Length of date for truncate.
     */
    private int truncateDateLength;


    public DataAggregator(AggregateGranularity granularity) {
        switch (granularity) {
            case DAY:
                truncateDateLength = 10;
                break;
            case MONTH:
                truncateDateLength = 7;
                break;
            default:
                throw new IllegalArgumentException("Invalid value of granularity argument: expected DAY or MONTH, " +
                        "actual is " + granularity);
        }
        this.granularity = granularity;
    }

    /**
     * Return granularity for aggregation.
     */
    public AggregateGranularity getGranularity() {
        return granularity;
    }

    /**
     * Appends the report line to the list and returns it.
     *
     * @param row the line of report.
     * @return Instance of the aggregated report line.
     */
    public ReportLine append(ReportLine row) {
        synchronized (this) {
            String usageInterval = truncDate(row.getUsageDate());
            row.setUsageDate(usageInterval);
            int index = Collections.binarySearch(reportLines, row, aggComparator);
            if (index < 0) {
                index = -index;
                if (index > reportLines.size()) {
                    reportLines.add(row);
                } else {
                    reportLines.add(index - 1, row);
                }
            } else {
                ReportLine found = reportLines.get(index);
                found.setUsage(found.getUsage() + row.getUsage());
                found.setCost(found.getCost() + row.getCost());
                return found;
            }
        }

        return row;
    }

    /**
     * Truncate given date for aggregates.
     *
     * @param date the date.
     * @return truncated date.
     */
    private String truncDate(String date) {
        if (date == null || date.length() <= truncateDateLength) {
            return date;
        }
        return date.substring(0, truncateDateLength);
    }

    /**
     * Returns the number of the report lines in list.
     */
    public int size() {
        return reportLines.size();
    }

    /**
     * Returns the report line.
     *
     * @param index index of the report line.
     */
    public ReportLine get(int index) {
        return reportLines.get(index);
    }

    /**
     * Removes all of the elements from list.
     */
    public void clear() {
        reportLines.clear();
    }

    /**
     * Comparator for aggregation.
     */
    private class AggComparator implements Comparator<ReportLine> {
        @Override
        public int compare(ReportLine o1, ReportLine o2) {
            if (o1 == null) {
                return (o2 == null ? 0 : -1);
            } else if (o2 == null) {
                return 1;
            }

            int result = StringUtils.compare(o1.getResourceId(), o2.getResourceId());
            if (result == 0) {
                result = StringUtils.compare(o1.getUsageType(), o2.getUsageType());
                if (result == 0) {
                    result = StringUtils.compare(o1.getUsageDate(), o2.getUsageDate());
                    if (result == 0) {
                        result = StringUtils.compare(o1.getProduct(), o2.getProduct());
                        if (result == 0) {
                            result = StringUtils.compare(o1.getUser(), o2.getUser());
                            if (result == 0) {
                                return StringUtils.compare(o1.getDatalabId(), o2.getDatalabId());
                            }
                        }
                    }
                }
            }
            return result;
        }
    }
}
