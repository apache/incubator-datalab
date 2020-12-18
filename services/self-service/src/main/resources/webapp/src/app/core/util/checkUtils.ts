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

import { PATTERNS } from './patterns';

export class CheckUtils {
  public static isJSON(str) {
    console.log(str);
    try {
      JSON.parse(str);
    } catch (e) {
      return false;
    }
    return true;
  }

  public static isNumberKey($event): boolean {
    const charCode = ($event.which) ? $event.which : $event.keyCode;
    if (charCode > 31 && (charCode < 48 || charCode > 57)
      && (charCode < 96 || charCode > 105)
      || charCode === 45 || charCode === 47 || charCode === 101) {
      $event.preventDefault();
      return false;
    }
    return true;
  }

  public static numberOnly(event): boolean {
    const charCode = (event.which) ? event.which : event.keyCode;
    if (charCode > 31 && (charCode < 48 || charCode > 57)) {
      return false;
    }
    return true;
  }

  public static delimitersFiltering(resource): string {
    return resource.replace(RegExp(PATTERNS.delimitersRegex, 'g'), '').toString().toLowerCase();
  }

  public static decodeUnicode(str) {
    str = str.replace(/\\/g, "%");
    return unescape(str);
  }

  public static endpointStatus = {
    CREATING: 'CONNECTING',
    STARTING: 'CONNECTING',
    RUNNING: 'CONNECTED',
    STOPPING: 'DISCONNECTING',
    STOPPED: 'DISCONNECTED'
  }
}
