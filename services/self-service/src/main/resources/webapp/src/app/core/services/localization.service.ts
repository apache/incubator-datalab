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
import * as _moment from 'moment';
import 'moment-timezone';


@Injectable({
  providedIn: 'root'
})
export class LocalizationService {
  public timezone = _moment().format('Z');
  private _locale = 'en';

  constructor() { }

  get locale() {
    this._locale = window.navigator.language;
    return this._locale;
  }

  public getTzOffset() {
    return this.timezone;
  }

  public static registerCulture(culture: string) {
    console.log(culture);
    if (culture === 'uk-UA' || culture === 'en-US') {
      culture = culture.substr(0, culture.indexOf('-'));
    }

    /* webpackInclude: /(uk|sv)\.js$/ */
    import(
      `@angular/common/locales/${culture}.js`
      ).then(module => registerLocaleData(module.default));
  }

  public getLocale() {
    return this.locale;
  }
}
