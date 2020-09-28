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

package com.epam.datalab.billing;

public class BillingCalculationUtils {
    private BillingCalculationUtils() {
    }

    public static String formatDouble(Double value) {
        return (value == null ? null : String.format("%,.2f", value));
    }

    public static double round(double value, int scale) {
        int d = (int) Math.pow(10, scale);
        return (double) (Math.round(value * d)) / d;
    }

    public static Double round(Double value, int scale) {
        if (value == null) {
            return null;
        }
        int d = (int) Math.pow(10, scale);
        return (double) (Math.round(value * d)) / d;
    }
}
