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

@Injectable()
export class StorageService {
  private accessTokenKey: string = 'access_token';
  private userNameKey: string = 'user_name';
  private quoteUsedKey: string = 'billing_quote';

  private readonly JWT_TOKEN = 'JWT_TOKEN';
  private readonly REFRESH_TOKEN = 'REFRESH_TOKEN';

  getToken(): string {
    return window.localStorage.getItem(this.JWT_TOKEN);
  }

  getRefreshToken() {
    return localStorage.getItem(this.REFRESH_TOKEN);
  }

  setAuthToken(token: string) {
    window.localStorage.setItem(this.JWT_TOKEN, token);
  }

  getUserName(): string {
    return window.localStorage.getItem(this.userNameKey);
  }

  setUserName(userName): void {
    window.localStorage.setItem(this.userNameKey, userName);
  }

  getBillingQuoteUsed(): string {
    return window.localStorage.getItem(this.quoteUsedKey);
  }

  setBillingQuoteUsed(quote): void {
    window.localStorage.setItem(this.quoteUsedKey, quote);
  }

  storeTokens(tokens) {
    window.localStorage.setItem(this.JWT_TOKEN, tokens.access_token);
    window.localStorage.setItem(this.REFRESH_TOKEN, tokens.refresh_token);
  }

  destroyAccessToken(): void {
    window.localStorage.removeItem(this.JWT_TOKEN);
  }

  destroyTokens(): void {
    window.localStorage.removeItem(this.JWT_TOKEN);
    window.localStorage.removeItem(this.REFRESH_TOKEN);
  }
}
