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

import { NgModule, Optional, SkipSelf, ModuleWithProviders } from '@angular/core';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

import { CommonModule } from '@angular/common';
import { ApplicationServiceFacade } from './services/applicationServiceFacade.service';
import { AppRoutingService } from './services/appRouting.service';
import { ApplicationSecurityService } from './services/applicationSecurity.service';
import { HealthStatusService } from './services/healthStatus.service';
import { UserResourceService } from './services/userResource.service';
import { AuthorizationGuard } from './services/authorization.guard';
import { CloudProviderGuard } from './services/cloudProvider.guard';
import { AdminGuard } from './services/admin.guard';
import { CheckParamsGuard } from './services/checkParams.guard';
import { LibrariesInstallationService } from './services/librariesInstallation.service';
import { ManageUngitService } from './services/manageUngit.service';
import { BillingReportService } from './services/billingReport.service';
import { BackupService } from './services/backup.service';
import { SchedulerService } from './services/scheduler.service';
import { ManageEnvironmentsService } from './services/managementEnvironments.service';
import { RolesGroupsService } from './services/rolesManagement.service';
import { DataengineConfigurationService } from './services/dataengineConfiguration.service';
import { StorageService } from './services/storage.service';
import { ProjectService } from './services/project.service';
import { EndpointService } from './services/endpoint.service';
import { UserAccessKeyService } from './services/userAccessKey.service';

import { HttpTokenInterceptor } from './interceptors/http.token.interceptor';
import { NoCacheInterceptor } from './interceptors/nocache.interceptor';
import { ErrorInterceptor } from './interceptors/error.interceptor';

import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import {  ConfigurationService } from './services/configutration.service';
import {AuditGuard, OdahuDeploymentService, ImagesPageService} from './services';
import {  ProjectAdminGuard } from './services/projectAdmin.guard';

@NgModule({
  imports: [CommonModule],
  declarations: [],
  exports: [],
  providers: []
})

export class CoreModule {

  static forRoot(): ModuleWithProviders<CoreModule> {
    return {
      ngModule: CoreModule,
      providers: [
        ApplicationSecurityService,
        AuthorizationGuard,
        AdminGuard,
        ProjectAdminGuard,
        AuditGuard,
        CloudProviderGuard,
        CheckParamsGuard,
        AppRoutingService,
        UserResourceService,
        HealthStatusService,
        LibrariesInstallationService,
        ManageUngitService,
        BillingReportService,
        BackupService,
        SchedulerService,
        ManageEnvironmentsService,
        RolesGroupsService,
        ApplicationServiceFacade,
        DataengineConfigurationService,
        StorageService,
        ProjectService,
        EndpointService,
        UserAccessKeyService,
        ConfigurationService,
        OdahuDeploymentService,
        ImagesPageService,

        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: [] },
        {
          provide: HTTP_INTERCEPTORS,
          useClass: ErrorInterceptor,
          multi: true,
        },
        {
          provide: HTTP_INTERCEPTORS,
          useClass: HttpTokenInterceptor,
          multi: true
        },
        {
          provide: HTTP_INTERCEPTORS,
          useClass: NoCacheInterceptor,
          multi: true,
        }
      ]
    };
  }

  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    if (parentModule)
      throw new Error('CoreModule is already loaded. Import it in the AppModule only');
  }
}
