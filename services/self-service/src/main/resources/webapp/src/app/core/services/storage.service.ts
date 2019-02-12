import { Injectable } from '@angular/core';

@Injectable()
export class StorageService {
  private accessTokenKey: string = 'access_token';
  private userNameKey: string = 'user_name';
  private quoteUsedKey: string = 'billing_quote';

  getToken(): string {
    return window.localStorage.getItem(this.accessTokenKey);
  }

  setAuthToken(token: string) {
    window.localStorage.setItem(this.accessTokenKey, token);
  }

  destroyToken(): void {
    window.localStorage.removeItem(this.accessTokenKey);
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
}
