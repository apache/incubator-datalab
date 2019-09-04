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

import { ModuleWithProviders } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { LoginComponent } from './login/login.module';
import { LayoutComponent } from './layout/layout.component'
import { ResourcesComponent } from './resources/resources.component';
import { AccessNotebookGuideComponent, PublicKeyGuideComponent } from './help';
import { NotFoundComponent } from './not-found/not-found.component';
import { AccessDeniedComponent } from './access-denied/access-denied.component';
import { ReportingComponent } from './reporting/reporting.component';
import { WebterminalComponent } from './webterminal/webterminal.component';
import { ManagementComponent } from './administration/management/management.component';
import { ProjectComponent } from './administration/project/project.component';
import { RolesComponent } from './administration/roles/roles.component';

import { AuthorizationGuard, CheckParamsGuard, CloudProviderGuard, AdminGuard } from './core/services';

const routes: Routes = [{
  path: 'login',
  component: LoginComponent
}, {
  path: '',
  canActivate: [CheckParamsGuard],
  component: LayoutComponent,
  children: [
    {
      path: '',
      redirectTo: 'resources_list',
      pathMatch: 'full'
    }, {
      path: 'resources_list',
      component: ResourcesComponent,
      canActivate: [CheckParamsGuard]
    }, {
      path: 'billing_report',
      component: ReportingComponent,
      canActivate: [AuthorizationGuard, CloudProviderGuard]
    }, {
      path: 'projects',
      component: ProjectComponent,
      canActivate: [AuthorizationGuard, AdminGuard],
    }, {
      path: 'roles',
      component: RolesComponent,
      canActivate: [AuthorizationGuard, AdminGuard],
    }, {
      path: 'environment_management',
      component: ManagementComponent,
      canActivate: [AuthorizationGuard, AdminGuard]
    }, {
      path: 'help/publickeyguide',
      component: PublicKeyGuideComponent,
      canActivate: [AuthorizationGuard]
    }, {
      path: 'help/accessnotebookguide',
      component: AccessNotebookGuideComponent,
      canActivate: [AuthorizationGuard]
    }
  ]
}, {
  path: 'terminal/:id',
  component: WebterminalComponent
}, {
  path: '403',
  component: AccessDeniedComponent,
  canActivate: [AuthorizationGuard]
}, {
  path: '**',
  component: NotFoundComponent
}];

export const AppRoutingModule: ModuleWithProviders = RouterModule.forRoot(routes, { useHash: true });
