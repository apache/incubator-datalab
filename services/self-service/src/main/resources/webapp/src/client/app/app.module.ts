/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import {RouterModule, Router, } from '@angular/router';
import {ConnectionBackend, HttpModule, Http, XHRBackend, RequestOptions} from '@angular/http';
import { AppComponent } from './app.component';
import { routes } from './app.routes';

import {
  LocationStrategy,
  HashLocationStrategy
} from '@angular/common';

import { AuthorizationGuard } from './security/authorization.guard';
import { LoginModule } from './login/login.module';
import { HomeModule } from './home/home.module';
import {FormsModule} from "@angular/forms";
import {UserAccessKeyService} from "./services/userAccessKey.service";
import {AppRoutingService} from "./routing/appRouting.service";
import {UserResourceService} from "./services/userResource.service";
import {HttpInterceptor} from "./util/interceptors/httpInterceptor.service";
import {ApplicationServiceFacade} from "./services/applicationServiceFacade.service";
import {ApplicationSecurityService} from "./services/applicationSecurity.service";

@NgModule({
  imports: [BrowserModule, HttpModule, RouterModule.forRoot(routes, { useHash: true }), LoginModule, HomeModule, FormsModule],
  declarations: [AppComponent],
  providers: [{
    provide: LocationStrategy,
    useClass: HashLocationStrategy,
    useValue: '<%= APP_BASE %>'
    },
    {
      provide: Http,
      useFactory: (backend: XHRBackend, defaultOptions: RequestOptions, router: Router) => {
      return new HttpInterceptor(backend, defaultOptions, router);
    },
    deps: [ XHRBackend, RequestOptions, Router]},
    AuthorizationGuard,
    ApplicationSecurityService,
    UserAccessKeyService,
    AppRoutingService,
    UserResourceService,
    ApplicationServiceFacade
    ],
  bootstrap: [AppComponent]
})

export class AppModule {}
