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

import { Injectable } from '@angular/core';
import { registerLocaleData } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class LocalizationService {
  private _locale;

  constructor() {

  }

  get locale() {

    if (!this._locale) {
      let locale = window.navigator.language;
      if (locale.indexOf('-') !== -1 && locale !== 'en-GB') {
        locale = locale.substr(0, locale.indexOf('-'));
      }
      this._locale = locale;
    }
    return this._locale;
  }

  public static registerCulture(culture: string) {
    if (culture.indexOf('-') !== -1 && culture !== 'en-GB') {
      culture = culture.substr(0, culture.indexOf('-'));
    }

    import(
      `@angular/common/locales/${culture}.js`
      ).then(module => registerLocaleData(module.default));

    if (culture !== 'en' && culture !== 'en-GB') {
      import(
        `@angular/common/locales/en-GB.js`
        ).then(module => registerLocaleData(module.default));
    }
  }
}
