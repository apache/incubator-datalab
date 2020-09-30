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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * List of usage dates the billing report data.
 */
public class UsageDataList implements Iterable<String> {
    /**
     * List of dates.
     */
    private final Map<String, Boolean> map = new HashMap<>();

    /**
     * Appends the date to the list and returns it.
     *
     * @param usageDate the date of data.
     * @return Instance of the range.
     */
    public void append(String usageDate) {
        synchronized (this) {
            if (!map.containsKey(usageDate)) {
                map.put(usageDate, false);
            }
        }
    }

    /**
     * Returns the number of the range in list.
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns the value for date.
     *
     * @param usageDate the date.
     */
    public Boolean get(String usageDate) {
        return map.get(usageDate);
    }

    /**
     * Set the value of usageDate.
     *
     * @param usageDate the date.
     */
    public void set(String usageDate, boolean value) {
        if (map.containsKey(usageDate)) {
            map.put(usageDate, value);
        }
    }

    /**
     * Removes all of the elements from list.
     */
    public void clear() {
        map.clear();
    }

    @Override
    public Iterator<String> iterator() {
        return map.keySet().iterator();
    }
}
