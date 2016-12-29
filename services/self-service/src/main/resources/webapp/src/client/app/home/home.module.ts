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
import { CommonModule } from '@angular/common';
import { HomeComponent } from './home.component';
import { ModalModule } from './../components/modal/index';
import { ResourcesGridModule } from './../components/resources-grid/index';

import { ProgressDialogModule } from './../components/progress-dialog/index';
import { UploadKeyDialogModule } from './../components/key-upload-dialog/index';
import { ExploratoryEnvironmentCreateDialogModule } from './../components/exploratory-environment-create-dialog/index';

import { NavbarModule } from './../shared/navbar/index';
import { ApplicationSecurityService } from '../services/applicationSecurity.service';

@NgModule({
  imports: [
    CommonModule,
    ModalModule,
    ResourcesGridModule,
    ProgressDialogModule,
    UploadKeyDialogModule,
    ExploratoryEnvironmentCreateDialogModule,
    NavbarModule
  ],
  declarations: [HomeComponent],
  exports: [HomeComponent],
  providers: [ApplicationSecurityService]
})
export class HomeModule { }
