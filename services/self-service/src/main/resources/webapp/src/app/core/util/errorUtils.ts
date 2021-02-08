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


import { throwError as observableThrowError } from 'rxjs';
import { CheckUtils } from './checkUtils';
import {logger} from 'codelyzer/util/logger';

export class ErrorUtils {

  // public static handleError(error: any) {
  //   let errMsg: string;
  //   if (typeof error === 'object' && error._body && CheckUtils.isJSON(error._body)) {
  //     if (error.json().error_message)
  //       errMsg = error.json().error_message;
  //   } else if (CheckUtils.isJSON(error._body)) {
  //     const body = error.json() || '';
  //     const err = body.error || JSON.stringify(body);
  //     errMsg = `${error.status} - ${error.statusText || ''} ${err}`;
  //   } else {
  //     errMsg = error._body ? error._body : error.toString();
  //   }

  //   return errMsg;
  // }

  public static handleServiceError(errorMessage) {
    const error = CheckUtils.isJSON(errorMessage.error) ? JSON.parse(errorMessage.error) : errorMessage.error;

    return observableThrowError({
      status: error.code,
      statusText: errorMessage.statusText,
      message: error.message
    });
  }
}
