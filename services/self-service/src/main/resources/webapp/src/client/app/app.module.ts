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
import { BrowserModule } from '@angular/platform-browser';
import { RouterModule, Router } from '@angular/router';
import { HttpModule, Http, XHRBackend, RequestOptions } from '@angular/http';
import { AppComponent } from './app.component';
import { routes } from './app.routes';

import { HomeModule } from './home/home.module';
import { LoginModule } from './login/login.module';
import { AccessNotebookGuideModule } from './help/accessnotebookguide/accessnotebookguide.module';
import { PublicKeyGuideModule } from './help/publickeyguide/publickeyguide.module';
import { NotFoundModule } from './not-found/not-found.module';

import { LocationStrategy, HashLocationStrategy } from '@angular/common';
import { AuthorizationGuard } from './security/authorization.guard';
import { FormsModule } from '@angular/forms';
import { UserAccessKeyService } from './services/userAccessKey.service';
import { AppRoutingService } from './routing/appRouting.service';
import { UserResourceService } from './services/userResource.service';
import { HttpInterceptor } from './util/interceptors/httpInterceptor.service';
import { ApplicationServiceFacade } from './services/applicationServiceFacade.service';
import { ApplicationSecurityService } from './services/applicationSecurity.service';

@NgModule({
  imports: [
    BrowserModule,
    HttpModule,
    RouterModule.forRoot(routes, { useHash: true }),
    FormsModule,
    HomeModule,
    LoginModule,
    AccessNotebookGuideModule,
    PublicKeyGuideModule,
    NotFoundModule
  ],
  declarations: [AppComponent],
  providers: [{
      provide: LocationStrategy,
      useClass: HashLocationStrategy,
      useValue: '<%= APP_BASE %>'
    }, {
      provide: Http,
      useFactory: (backend: XHRBackend, defaultOptions: RequestOptions, router: Router) => {
        return new HttpInterceptor(backend, defaultOptions, router);
      },
      deps: [XHRBackend, RequestOptions, Router]
    },
    AuthorizationGuard,
    ApplicationSecurityService,
    UserAccessKeyService,
    AppRoutingService,
    UserResourceService,
    ApplicationServiceFacade
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
