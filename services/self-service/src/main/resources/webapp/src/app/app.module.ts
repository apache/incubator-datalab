/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { BrowserModule } from '@angular/platform-browser';
import { LocationStrategy, HashLocationStrategy } from '@angular/common';
import { HttpModule, Http, XHRBackend, RequestOptions } from '@angular/http';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app.routing.module';

import { LoginModule } from './login/login.module';
import { GuidesModule } from './help';
import { NotFoundModule } from './not-found/not-found.module';
import { ResourcesModule } from './resources/resources.module';
import { HealthStatusModule } from './health-status/health-status.module';
import { LogInterceptorFactory } from './core/interceptors/logInterceptor.factory';
import { ReportingModule } from './reporting/reporting.module';

import {
  UserAccessKeyService,
  UserResourceService,
  HealthStatusService,
  AppRoutingService,
  AuthorizationGuard,
  CloudProviderGuard,
  ApplicationServiceFacade,
  ApplicationSecurityService,
  LibrariesInstallationService,
  ManageUngitService,
  BillingReportService
} from './core/services';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    LoginModule,
    ResourcesModule,
    GuidesModule,
    NotFoundModule,
    HealthStatusModule,
    ReportingModule,
    RouterModule,
    AppRoutingModule
  ],
  providers: [{
      provide: LocationStrategy,
      useClass: HashLocationStrategy,
      useValue: '/'
    }, {
      provide: Http,
      useFactory: LogInterceptorFactory,
      deps: [XHRBackend, RequestOptions, Router]
    },
    AuthorizationGuard,
    CloudProviderGuard,
    ApplicationSecurityService,
    UserAccessKeyService,
    AppRoutingService,
    UserResourceService,
    HealthStatusService,
    LibrariesInstallationService,
    ManageUngitService,
    BillingReportService,
    ApplicationServiceFacade
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
