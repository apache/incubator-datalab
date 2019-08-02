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

import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { BrowserModule } from '@angular/platform-browser';
import { LocationStrategy, HashLocationStrategy } from '@angular/common';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app.routing.module';

import { LoginModule } from './login/login.module';
import { NavbarModule } from './shared/navbar';
import { GuidesModule } from './help';
import { NotFoundModule } from './not-found/not-found.module';
import { AccessDeniedModule } from './access-denied/access-denied.module';
import { ResourcesModule } from './resources/resources.module';
import { HttpTokenInterceptor } from './core/interceptors/http.token.interceptor';
import { NoCacheInterceptor } from './core/interceptors/nocache.interceptor';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';

import { ReportingModule } from './reporting/reporting.module';
import { ManagenementModule } from './management';
import { WebterminalModule } from './webterminal';

import { CoreModule } from './core/core.module';
import { ToastrModule } from 'ngx-toastr';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    LoginModule,
    NavbarModule,
    ResourcesModule,
    GuidesModule,
    NotFoundModule,
    AccessDeniedModule,
    ReportingModule,
    ManagenementModule,
    WebterminalModule,
    RouterModule,
    AppRoutingModule,
    CoreModule.forRoot(),
    ToastrModule.forRoot({
      timeOut: 10000
    })
  ],
  providers: [{
      provide: LocationStrategy,
      useClass: HashLocationStrategy,
      useValue: '/'
    }, {
      provide: HTTP_INTERCEPTORS,
      useClass: HttpTokenInterceptor,
      multi: true
    }, {
      provide: HTTP_INTERCEPTORS,
      useClass: NoCacheInterceptor,
      multi: true,
    }, {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true,
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
