/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

 import { Component, OnInit, ViewChild, Input, Output, EventEmitter } from '@angular/core';
 import { UserResourceService } from "./../../services/userResource.service";
 import { Modal } from './../modal/modal.component';

 @Component({
   moduleId: module.id,
   selector: 'confirmation-dialog',
   templateUrl: 'confirmation-dialog.component.html'
 })

 export class confirmationDialog {
   notebook: any;
   stopText:string = `Notebook server will be stopped and all connected EMR instances will be terminated.`;
   terminateText:string = `Notebook server and all connected EMR instances will be terminated.`;
   action:string;


   @ViewChild('bindDialog') bindDialog;
   @Output() buildGrid: EventEmitter<{}> = new EventEmitter();


   constructor(
    private userResourceService: UserResourceService
    ) { }

   open(param, notebook, action) {
     this.bindDialog.open(param);
     this.notebook = notebook;
     this.action = action;
   }
   close() {
     this.bindDialog.close();
   }

   stop() {
     let url = "/" + this.notebook.name + "/stop";

     this.userResourceService
        .suspendExploratoryEnvironment(url)
        .subscribe((result) => {
          console.log('stopUsernotebook result: ', result);

          this.close();
          this.buildGrid.emit();
        });
   }

   terminate(){
     let url = "/" + this.notebook.name + "/terminate";

     this.userResourceService
        .suspendExploratoryEnvironment(url)
        .subscribe((result) => {
          console.log('terminateUsernotebook result: ', result);

          this.close();
          this.buildGrid.emit();
        });
   }
 }
