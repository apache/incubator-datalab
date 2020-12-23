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

import {NgModule} from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {LocationStrategy, HashLocationStrategy, APP_BASE_HREF} from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ToastrModule } from 'ngx-toastr';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app.routing.module';

import { LoginModule } from './login/login.module';
import { LayoutModule } from './layout/layout.module';

import { GuidesModule } from './help';
import { ServicePagesModule } from './service-pages/service-pages.module';
import { ResourcesModule } from './resources/resources.module';
import { AdministrationModule } from './administration/administration.module';
import { WebterminalModule } from './webterminal';
import { CoreModule } from './core/core.module';
import { SwaggerAPIModule } from './swagger';
import {ReportsModule} from './reports/reports.module';
import {LocalizationService} from './core/services/localization.service';
import {AceEditorModule} from 'ng2-ace-editor';

LocalizationService.registerCulture(window.navigator.language);

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    LoginModule,
    LayoutModule,
    ResourcesModule,
    GuidesModule,
    ServicePagesModule,
    // ReportingModule,

    AdministrationModule,
    ReportsModule,
    WebterminalModule,
    SwaggerAPIModule,
    RouterModule,
    AppRoutingModule,
    CoreModule.forRoot(),
    ToastrModule.forRoot({ timeOut: 10000 }),
    AceEditorModule
  ],
  providers: [{
    provide: LocationStrategy,
    useClass: HashLocationStrategy,
    useValue: '/'
  },

    // { provide: LOCALE_ID,
    //   deps: [LocalizationService],
    //   useFactory: (localizationService) => localizationService.getLocale() }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
