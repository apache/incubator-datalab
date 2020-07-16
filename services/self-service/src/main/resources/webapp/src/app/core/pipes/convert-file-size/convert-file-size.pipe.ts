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

import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'convertFileSize' })

export class ConvertFileSizePipe implements PipeTransform {
  transform(bytes: number): any {
      const sizes = ['bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];
      if (bytes === 0) {
        return '0 byte';
      }
      for (let i = 0; i < sizes.length; i++) {
        if (bytes <= 1024) {
          return bytes + ' ' + sizes[i];
        } else {
          bytes = parseFloat((bytes / 1024).toFixed(2));
        }
      }
      return bytes;
    }
}
