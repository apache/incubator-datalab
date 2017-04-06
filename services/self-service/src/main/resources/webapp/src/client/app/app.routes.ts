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

import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { HealthStatusComponent } from './health-status/health-status.component';
import { LoginComponent } from './login/login.component';
import { AccessNotebookGuide } from './help/accessnotebookguide/accessnotebookguide.component';
import { PublicKeyGuide } from './help/publickeyguide/publickeyguide.component';
import { AuthorizationGuard } from './security/authorization.guard';
import { NotFoundComponent } from './not-found/not-found.component';

export const routes: Routes = [{
    path: 'login',
    component: LoginComponent
  }, {
    path: 'resources_list',
    component: HomeComponent,
    canActivate: [AuthorizationGuard]
  }, {
    path: 'environment_health_status',
    component: HealthStatusComponent,
    canActivate: [AuthorizationGuard]
  }, {
    path: 'help/accessnotebookguide',
    component: AccessNotebookGuide,
    canActivate: [AuthorizationGuard]
  }, {
    path: 'help/publickeyguide',
    component: PublicKeyGuide,
    canActivate: [AuthorizationGuard]
  }, {
    path: '',
    redirectTo: 'resources_list',
    pathMatch: 'full'
  }, {
    path: '**',
    component: NotFoundComponent
  }];
