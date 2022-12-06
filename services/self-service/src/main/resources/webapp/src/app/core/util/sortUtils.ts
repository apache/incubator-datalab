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

export class SortUtils {
  public static statusSort(arg1: string, arg2: string): number {
    const order = ['creating', 'running', 'stopping', 'stopped', 'terminating', 'terminated', 'failed'];

    return order.indexOf(arg1) - order.indexOf(arg2);
  }

  public static activeStatuses(): String[] {
    return ['running', 'stopping', 'stopped', 'creating', 'configuring', 'reconfiguring', 'starting', 'creating image'];
  }

  public static shapesSort(shapesJson) {
    const sortOrder = ['For testing', 'Memory optimized', 'GPU optimized', 'Compute optimized'];
    const sortedShapes = {};

    Object.keys(shapesJson)
      .sort((a, b) => sortOrder.indexOf(a) - sortOrder.indexOf(b))
      .forEach(key => {
        if (shapesJson[key].length) {
          sortedShapes[key] = shapesJson[key];
        }
      });

    return sortedShapes;
  }

  public static libGroupsSort(groups) {
    const sortOrder = ['os_pkg', 'pip3', 'r_pkg', 'java', 'others'];

    return groups.sort((arg1, arg2) => sortOrder.indexOf(arg1) - sortOrder.indexOf(arg2));
  }

  public static libFilterGroupsSort(groups) {
    const sortOrder = ['Apt/Yum', 'Python 3', 'R packages', 'Java', 'Others'];

    return groups.sort((arg1, arg2) => sortOrder.indexOf(arg1) - sortOrder.indexOf(arg2));
  }

  public static flatDeep(arr, d = 1) {
    return d > 0
      ? arr.reduce((acc, val) => acc.concat(Array.isArray(val) ? this.flatDeep(val, d - 1) : val), [])
      : arr.slice();
  }

  public static sortByKeys(array, keys) {
    keys.forEach(key => {
      array = array.sort((a, b) => (a[key] > b[key]) ? 1 : ((b[key] > a[key]) ? -1 : 0));
    });
    return array;
  }

}
