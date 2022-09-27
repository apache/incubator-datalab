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
import { RouterModule, Routes } from '@angular/router';

import { LoginComponent } from './login/login.module';
import { LayoutComponent } from './layout/layout.component';
import { ResourcesComponent } from './resources/resources.component';
import { AccessNotebookGuideComponent, PublicKeyGuideComponent } from './help';
import { NotFoundComponent } from './service-pages/not-found/not-found.component';
import { AccessDeniedComponent } from './service-pages/access-denied/access-denied.component';
import { WebterminalComponent } from './webterminal/webterminal.component';
import { ManagementComponent } from './administration/management';
import { ProjectComponent } from './administration/project/project.component';
import { RolesComponent } from './administration/roles/roles.component';
import { SwaggerComponent } from './swagger';
import { AdminGuard, AuditGuard, AuthorizationGuard, CheckParamsGuard, CloudProviderGuard, ImagePageResolveGuard } from './core/services';
import { ConfigurationComponent } from './administration/configuration/configuration.component';
import { ProjectAdminGuard } from './core/services/projectAdmin.guard';
import { ReportingComponent } from './reports/reporting/reporting.component';
import { AuditComponent } from './reports/audit/audit.component';
import { ImagesComponent } from './resources/images/images.component';
import { RoutingListConfig } from './core/configs/routing-list.config';

const routes: Routes = [
  {
    path: RoutingListConfig.login,
    component: LoginComponent
  },
  {
    path: '',
    canActivate: [CheckParamsGuard],
    component: LayoutComponent,
    children: [
      {
        path: '',
        redirectTo: RoutingListConfig.instances,
        pathMatch: 'full'
      },
      {
        path: RoutingListConfig.instances,
        component: ResourcesComponent,
        canActivate: [AuthorizationGuard]
      },
      {
        path: RoutingListConfig.images,
        component: ImagesComponent,
        canActivate: [AuthorizationGuard],
        resolve: {
          projectList: ImagePageResolveGuard
        },
      },
      {
        path: RoutingListConfig.connectedPlatforms,
        loadChildren: () => import('./resources/connected-platforms/connected-platforms.module').then(m => m.ConnectedPlatformsModule)
      },
      {
        path: RoutingListConfig.billing,
        component: ReportingComponent,
        canActivate: [AuthorizationGuard, CloudProviderGuard]
      },
      {
        path: RoutingListConfig.projects,
        component: ProjectComponent,
        canActivate: [AuthorizationGuard, AdminGuard],
      },
      {
      //   path: 'odahu',
      //   component: OdahuComponent,
      //   canActivate: [AuthorizationGuard, AdminGuard],
      // }, {
        path: RoutingListConfig.users,
        component: RolesComponent,
        canActivate: [AuthorizationGuard, AdminGuard],
      },
      {
        path: RoutingListConfig.resources,
        component: ManagementComponent,
        canActivate: [AuthorizationGuard, AdminGuard]
      },
      {
        path: RoutingListConfig.configuration,
        component: ConfigurationComponent,
        canActivate: [AuthorizationGuard, AdminGuard, ProjectAdminGuard]
      },
      {
        path: RoutingListConfig.swagger,
        component: SwaggerComponent,
        canActivate: [AuthorizationGuard]
      },
      {
        path: RoutingListConfig.publickeyguide,
        component: PublicKeyGuideComponent,
        canActivate: [AuthorizationGuard]
      },
      {
        path: RoutingListConfig.accessnotebookguide,
        component: AccessNotebookGuideComponent,
        canActivate: [AuthorizationGuard]
      },
      {
        path: RoutingListConfig.audit,
        component: AuditComponent,
        canActivate: [AuthorizationGuard, AuditGuard],
      },
    ]
  },
  {
    path: 'terminal/:id/:endpoint',
    component: WebterminalComponent
  },
  {
    path: '403',
    component: AccessDeniedComponent,
    canActivate: [AuthorizationGuard]
  },
  {
  path: '**',
  component: NotFoundComponent
  }
];

export const AppRoutingModule: ModuleWithProviders<RouterModule> = RouterModule.forRoot(routes, { useHash: true, relativeLinkResolution: 'corrected' });
