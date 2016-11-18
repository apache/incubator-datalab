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
   selector: 'detail-computational-resources',
   templateUrl: 'detail-computational-resources.component.html'
 })

 export class DetailComputationalResources {
   resource: any;
   environment: any;
   @ViewChild('bindDialog') bindDialog;

   upTimeInHours: number ;
   upTimeSince: string = "";

   open(param, environment, resource) {
     this.resource = resource;
     this.environment = environment;
     if(this.resource.up_time){
      this.upTimeInHours = DateUtils.diffBetweenDatesInHours(this.resource.up_time);
      this.upTimeSince = new Date(this.resource.up_time).toString();
     }
     this.bindDialog.open(param);
   }
 }

