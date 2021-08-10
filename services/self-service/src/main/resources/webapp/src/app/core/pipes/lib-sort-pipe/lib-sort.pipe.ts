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

import { Pipe } from '@angular/core';

@Pipe({name: 'libStatusSort', pure: false})

export class LibSortPipe {
  transform(array: Array<Object>): Array<Object> {
    const order = ['installing', 'installed', 'failed'];
    array.sort((arg1: any, arg2: any) => {
      if (arg1.status !== arg2.status)
        return order.indexOf(arg1.status) - order.indexOf(arg2.status);
      else
        return arg1.name !== arg2.name ? arg1.name < arg2.name ? -1 : 1 : 0;
    });
    return array;
  }
}
