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

import { Component, OnInit, ViewChild, Input } from '@angular/core';
import { Modal } from './../modal/modal.component';
import {DateUtils} from './../../util/dateUtils'


 @Component({
   moduleId: module.id,
   selector: 'detail-dialog',
   templateUrl: 'detail-dialog.component.html'
 })

 export class DetailDialog {
   notebook: any;
   upTimeInHours: number;
   upTimeSince: string = "";
   @ViewChild('bindDialog') bindDialog;

 	open(param, notebook) {
    this.notebook = notebook;

    if(notebook.time) {
      this.upTimeInHours = DateUtils.diffBetweenDatesInHours(this.notebook.time);
      this.upTimeSince = new Date(this.notebook.time).toString();
    }

    this.bindDialog.open(param);
   }
 }
