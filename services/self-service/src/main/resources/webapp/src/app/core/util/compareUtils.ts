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

export class CompareUtils {
  public static compareFilters(currentData, previousData) {
    return Object.keys(currentData).every(el => {
      if (Array.isArray(previousData[el])) {
        // console.log('arr', el, previousData[el], currentData[el]);
        if (previousData[el].length === 0 && currentData[el].length === 0) return true;
        if (previousData[el].length === currentData[el].length) {
          return currentData[el].every(element => previousData[el].includes(element));
        } else {
          return false;
        }
      } else {
        // console.log(el, previousData[el] === currentData[el]);
        return previousData[el] === currentData[el];
      }
    });
  }

}
