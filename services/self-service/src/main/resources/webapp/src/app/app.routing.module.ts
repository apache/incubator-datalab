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

import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { LoginComponent } from './login/login.module';
import { ResourcesComponent } from './resources/resources.component';
import { HealthStatusComponent } from './health-status/health-status.component';
import { AccessNotebookGuideComponent, PublicKeyGuideComponent } from './help';
import { NotFoundComponent } from './not-found/not-found.component';
import { ReportingComponent } from './reporting/reporting.component';
import { ManagementComponent } from './management/management.component';
import { AuthorizationGuard, CheckParamsGuard, CloudProviderGuard } from './core/services';

const routes: Routes = [{
    path: 'login',
    component: LoginComponent
  }, {
    path: 'resources_list',
    component: ResourcesComponent,
    canActivate: [CheckParamsGuard]
  }, {
    path: 'environment_health_status',
    component: HealthStatusComponent,
    canActivate: [AuthorizationGuard]
  }, {
    path: 'billing_report',
    component: ReportingComponent,
    canActivate: [AuthorizationGuard, CloudProviderGuard]
  }, {
    path: 'environment_management',
    component: ManagementComponent,
    canActivate: [AuthorizationGuard]
  }, {
    path: 'help/publickeyguide',
    component: PublicKeyGuideComponent,
    canActivate: [AuthorizationGuard]
  }, {
    path: 'help/accessnotebookguide',
    component: AccessNotebookGuideComponent,
    canActivate: [AuthorizationGuard]
  }, {
    path: '',
    redirectTo: 'resources_list',
    pathMatch: 'full'
  }, {
    path: '**',
    component: NotFoundComponent
  }];

export const AppRoutingModule: ModuleWithProviders = RouterModule.forRoot(routes, { useHash: true });
