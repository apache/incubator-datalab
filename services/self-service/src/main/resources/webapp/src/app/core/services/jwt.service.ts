import { Injectable } from '@angular/core';

@Injectable()
export class JwtService {

  getToken(): string {
    return window.localStorage['access_token'];
  }

  saveToken(token: string) {
    window.localStorage['access_token'] = token;
  }

  destroyToken(): void {
    window.localStorage.removeItem('access_token');
  }
}
